"""
# Match-specific TBA data-fetching functions
"""

from tba_analysis.analysis_func import fetch_tba_json, log_msg

__all__ = [
    "get_match_details",
    "get_pre_match_rankings",
    "get_post_match_rankings",
    "get_match_rank_deltas",
]


def get_match_details(event_endpoint : str, match_type = "qm"):
    matches_endpoint = f"{event_endpoint}/matches"
    # contains a basic scoring breakdown for each match at the event
    match_list = []

    matches = fetch_tba_json(matches_endpoint)
    for match in matches:
        if(match['comp_level'] == match_type):
            match_num = match['match_number']
            red_teams = []
            blue_teams = []
            blue_score = match['score_breakdown'].get('blue',{}).get('totalPoints', 0)
            red_score = match['score_breakdown'].get('red',{}).get('totalPoints', 0)

            for team_id in match['alliances']['blue']['team_keys']:
                blue_teams.append(team_id.replace("frc", ""))

            for team_id in match['alliances']['red']['team_keys']:
                red_teams.append(team_id.replace("frc", ""))

            match_list.append([match_num, red_teams, red_score, blue_teams, blue_score])

    match_list.sort(key=lambda x: x[0])
    # print(match_list)
    return match_list


def _accumulate_match_stats(event_endpoint, through_match_num=None, match_type="qm"):
    """
    Walks an event's matches (sorted by match number) and accumulates each team's
    RP/score totals through and including `through_match_num`. Pass None to include
    every played match of `match_type`.
    """
    matches_endpoint = f"{event_endpoint}/matches"
    matches = fetch_tba_json(matches_endpoint)

    relevant_matches = [m for m in matches if m['comp_level'] == match_type]
    relevant_matches.sort(key=lambda m: m['match_number'])

    team_stats = {}

    for match in relevant_matches:
        match_num = match['match_number']
        if through_match_num is not None and match_num > through_match_num:
            break

        # unplayed matches have no score breakdown yet, so skip them
        if match.get('score_breakdown') is None:
            continue

        for alliance_color in ('blue', 'red'):
            alliance_rp = match['score_breakdown'].get(alliance_color, {}).get('rp', 0)
            alliance_score = match['alliances'][alliance_color].get('score', 0)

            for team_id in match['alliances'][alliance_color]['team_keys']:
                stats = team_stats.setdefault(team_id, {'rp': 0, 'score': 0, 'matches_played': 0})
                stats['rp'] = stats['rp'] + alliance_rp
                stats['score'] = stats['score'] + alliance_score
                stats['matches_played'] = stats['matches_played'] + 1

    return team_stats


def _rank_team_stats(team_stats):
    """
    Ranks teams by RP average (descending), using average alliance score as a
    tiebreaker. TBA's real per-game tiebreakers aren't modeled here.
    """
    ranking_list = []
    for team_id, stats in team_stats.items():
        matches_played = stats['matches_played']
        rp_avg = stats['rp'] / matches_played if matches_played else 0
        score_avg = stats['score'] / matches_played if matches_played else 0
        ranking_list.append({
            'team': team_id,
            'rp_total': stats['rp'],
            'rp_avg': rp_avg,
            'score_avg': score_avg,
            'matches_played': matches_played,
        })

    ranking_list.sort(key=lambda t: (t['rp_avg'], t['score_avg']), reverse=True)

    for position, team_rank in enumerate(ranking_list, start=1):
        team_rank['rank'] = position

    return ranking_list


def get_pre_match_rankings(event_endpoint, match_number, match_type="qm"):
    """
    Ranking of every team at the event using only matches completed before `match_number`.
    """
    log_msg(f"Computing pre-match rankings for match {match_number} at {event_endpoint}", level=2)

    team_stats = _accumulate_match_stats(event_endpoint, through_match_num=match_number - 1, match_type=match_type)
    return _rank_team_stats(team_stats)


def get_post_match_rankings(event_endpoint, match_number, match_type="qm"):
    """
    Ranking of every team at the event using every match completed through `match_number`.
    """
    log_msg(f"Computing post-match rankings for match {match_number} at {event_endpoint}", level=2)

    team_stats = _accumulate_match_stats(event_endpoint, through_match_num=match_number, match_type=match_type)
    return _rank_team_stats(team_stats)


def get_match_rank_deltas(event_endpoint, match_number, match_type="qm"):
    """
    Computes each team's rank change (pre-match rank minus post-match rank) caused by
    `match_number`. Positive delta means the team moved up in rank; negative means it
    dropped. Teams with no prior matches (pre_rank is None) can't have a delta computed.
    """
    pre_rankings = get_pre_match_rankings(event_endpoint, match_number, match_type=match_type)
    post_rankings = get_post_match_rankings(event_endpoint, match_number, match_type=match_type)

    pre_rank_by_team = {team['team']: team['rank'] for team in pre_rankings}

    deltas = []
    for team_rank in post_rankings:
        team_id = team_rank['team']
        pre_rank = pre_rank_by_team.get(team_id)
        post_rank = team_rank['rank']
        delta = None if pre_rank is None else pre_rank - post_rank
        deltas.append({
            'team': team_id,
            'pre_rank': pre_rank,
            'post_rank': post_rank,
            'delta': delta,
        })

        log_msg(f"TEAM {team_id}: pre-rank {pre_rank} -> post-rank {post_rank} (delta {delta})", level=2)

    deltas.sort(key=lambda t: t['post_rank'])
    return deltas
