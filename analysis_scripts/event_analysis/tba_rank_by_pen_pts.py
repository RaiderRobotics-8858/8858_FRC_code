import argparse

from tba_analysis.analysis_func import *

class CustomHelpFormatter(
    argparse.RawDescriptionHelpFormatter,
    argparse.ArgumentDefaultsHelpFormatter,
    argparse.MetavarTypeHelpFormatter):
    pass

help_text = """
Wow, you must be really desperate to come here...
"""

team_pen_diff_list = []
team_pen_list = []

def get_event_teams(event_endpoint):
    """
    fetces a list of teams at an event
    initializes lists used for analysis
    """
    global team_pen_list, team_pen_diff_list
    team_pen_list = []
    team_pen_diff_list = []
    teams_endpoint = f"{event_endpoint}/teams"
    team_list = fetch_tba_json(teams_endpoint)
    log_msg(teams_endpoint, level=2)
    log_msg("\nTeams at the event:", level=2)
    for team in team_list:
        log_msg(f"TEAM {team['team_number']}: {team['nickname']}", level=2)
        team_pen_diff_list.append([f"frc{team['team_number']}", 0, 0])
        team_pen_list.append([f"frc{team['team_number']}", 0, 0, 0])

def get_pen_pts_ranks_by_team(event_endpoint, team_num):
    """
    Finds the rankings of teams for a given event by the differential
    of penalty points their alliance gave up minus the penalty points their alliance
    was awarded at this event.
    """
    matches_endpoint = f"{event_endpoint}/matches"
    team_key = f"frc{team_num}"
    total_ranked_list = []
    diff_ranked_list = []
    match_breakdown = []
    get_event_teams(event_endpoint)
    matches = fetch_tba_json(matches_endpoint)
    log_msg(matches_endpoint, level=2)
    log_msg("\nMatches at the Event:", level=2)

    info_msg(f"\nBreakdown of matches for team #{team_num}:")
    for match in matches:
        if match['comp_level'] == "qm":
            match_num = match['match_number']

            # Check if 'score_breakdown' exists and is not None
            if match.get('score_breakdown') is not None:
                blue_penalty_pts = match['score_breakdown'].get('red', {}).get('foulPoints', 0)
                red_penalty_pts = match['score_breakdown'].get('blue', {}).get('foulPoints', 0)
            else:
                blue_penalty_pts = 0
                red_penalty_pts = 0

            log_msg(f"\nMATCH NUMBER: {match_num}", level=2)
            log_msg(f"BLUE PEN PTS: {blue_penalty_pts}", level=2)
            log_msg(f"RED PEN PTS:  {red_penalty_pts}", level=2)

            for team_id in match['alliances']['blue']['team_keys']:
                if team_id == team_key:
                    diff = blue_penalty_pts - red_penalty_pts
                    match_breakdown.append({
                        'match_num': match_num,
                        'against_team': blue_penalty_pts,
                        'against_opponent': red_penalty_pts,
                        'difference': diff
                    })
                    info_msg(f"Match #{match_num}:\t{blue_penalty_pts} pts against {team_num}, \t{red_penalty_pts} pts against opponent\t-> {diff} points")
                for i in range(len(team_pen_diff_list)):
                    if team_pen_diff_list[i][0] == team_id:
                        team_pen_list[i][1] = team_pen_list[i][1] + blue_penalty_pts
                        team_pen_list[i][2] = team_pen_list[i][2] + blue_penalty_pts - red_penalty_pts
                        team_pen_list[i][3] = team_pen_list[i][3] + 1

            for team_id in match['alliances']['red']['team_keys']:
                if team_id == team_key:
                    diff = red_penalty_pts - blue_penalty_pts
                    match_breakdown.append({
                        'match_num': match_num,
                        'against_team': red_penalty_pts,
                        'against_opponent': blue_penalty_pts,
                        'difference': diff
                    })
                    info_msg(f"Match #{match_num}:\t{red_penalty_pts} pts against {team_num}, \t{blue_penalty_pts} pts against opponent\t-> {diff} points")
                for i in range(len(team_pen_diff_list)):
                    if team_pen_diff_list[i][0] == team_id:
                        team_pen_list[i][1] = team_pen_list[i][1] + red_penalty_pts
                        team_pen_list[i][2] = team_pen_list[i][2] + red_penalty_pts - blue_penalty_pts
                        team_pen_list[i][3] = team_pen_list[i][3] + 1

    team_pen_list.sort(key=lambda x: x[1])
    i = 1
    info_msg("\n\nTOTAL PENALTY POINTS RANKING (lower is better)")
    for team, pen_score, pen_diff, num_matches in team_pen_list:
        total_ranked_list.append([team, pen_score])
        if team == team_key:
            info_msg(f"{i}.\tTEAM ID: {team}   \tPENALTY SCORE: {pen_score} \t# MATCHES: {num_matches}")
            total_ranking = i
        else:
            log_msg(f"{i}.\tTEAM ID: {team}   \tPENALTY SCORE: {pen_score} \t# MATCHES: {num_matches}", level=2)
        i = i + 1

    team_pen_list.sort(key=lambda x: x[2])
    i = 1
    info_msg("\n\nPENALTY POINTS DIFFERENTIAL RANKING (lower is better)")
    for team, pen_score, pen_diff, num_matches in team_pen_list:
        diff_ranked_list.append([team, pen_diff])
        if team == team_key:
            info_msg(f"{i}.\tTEAM ID: {team}   \tPENALTY DIFFERENTIAL: {pen_diff} \t# MATCHES: {num_matches}")
            diff_ranking = i
        else:
            log_msg(f"{i}.\tTEAM ID: {team}   \tPENALTY DIFFERENTIAL: {pen_diff} \t# MATCHES: {num_matches}", level=2)
        i = i + 1

    return total_ranked_list, total_ranking, diff_ranked_list, diff_ranking, match_breakdown

def main():

    parser = argparse.ArgumentParser(
        formatter_class=CustomHelpFormatter,
        description=help_text
    )

    parser.add_argument("-t", "-team_num", dest = "team_num", type = str, default=["prompt in script"], nargs = 1, help="Team Number to analyze")
    parser.add_argument("-y", "-year", dest = "year", type = str, default=["prompt in script"], nargs = 1, help="Year to fetch events from")
    parser.add_argument("-e", "-evnum", dest = "evnum", type = str, default=["prompt in script"], nargs = 1, help="Event # to fetch for")
    parser.add_argument("-v", "--verbosity", dest = "verbosity", type = int, default = 1, choices = [0, 1, 2], help="0=quiet, 1=normal (default), 2=verbose/debug")
    parser.add_argument("-d", "-debug_mode", dest = "debug_mode", action = 'store_true', help="[deprecated] alias for -v 2")

    # assign command line argument variables to their respective variables in the script
    args = parser.parse_args()
    team_num    = args.team_num[0]
    year        = args.year[0]
    evnum       = args.evnum[0]
    set_verbosity(2 if args.debug_mode else args.verbosity)

    if team_num == "prompt in script":
        team_num = input(f"Please Enter a Team Number: ")
    team_num = int(team_num)

    if year == "prompt in script":
        year = input(f"Please Enter a Year: ")
    year = int(year)

    # Endpoint for team information
    team_endpoint = f"{BASE_URL}/team/frc{team_num}"
    team_events = f"{team_endpoint}/events/{year}"

    log_msg(team_endpoint, level=2)
    log_msg(team_events, level=2)

    # Check that a team exists
    validate_team(team_endpoint)

    # get ID codes for each event a team participated this year
    event_ids, evnum = get_event_id_list(team_events, evnum)

    evnum = int(evnum)

    # sanity check that the event number is in the index of possible events
    if evnum > (len(event_ids)):
        error_msg(f"Event number {evnum} selected but only {len(event_ids)} were detected at {team_events}")
        exit()

    event_endpoint = f"{BASE_URL}/event/{event_ids[evnum - 1]}"
    log_msg(event_endpoint, level=2)
    event_data = fetch_tba_json(event_endpoint)
    info_msg(f"\nEvent Name: {event_data['name']}")
    info_msg(f"Location: {event_data['city']}, {event_data['state_prov']}, {event_data['country']}")

    total_ranked_list, total_ranking, diff_ranked_list, diff_ranking, match_breakdown = get_pen_pts_ranks_by_team(event_endpoint=event_endpoint, team_num=team_num)
    info_msg(f"Team {team_num} ranked {total_ranking}/{len(total_ranked_list)} in overall penalty points given and {diff_ranking}/{len(diff_ranked_list)} in penalty point differential")
    if VERBOSITY >= 2:
        get_event_matches(event_endpoint=event_endpoint, team_num=team_num)

if __name__ == "__main__":
    clear()
    main()