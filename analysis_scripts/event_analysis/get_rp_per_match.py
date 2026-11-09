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
team_rp_list = []

def get_event_teams(event_endpoint):
    """
    fetces a list of teams at an event
    initializes lists used for analysis
    """
    global team_rp_list
    team_rp_list = []
    teams_endpoint = f"{event_endpoint}/teams"
    team_list = fetch_tba_json(teams_endpoint)
    log_msg(teams_endpoint, level=2)
    log_msg("\nTeams at the event:", level=2)
    for team in team_list:
        log_msg(f"TEAM {team['team_number']}: {team['nickname']}", level=2)
        team_rp_list.append([f"frc{team['team_number']}", [], [], 0])

def get_rp_per_match(event_endpoint, team_num):
    matches_endpoint = f"{event_endpoint}/matches"
    team_key = f"frc{team_num}"
    total_ranked_list = []
    get_event_teams(event_endpoint)
    matches = fetch_tba_json(matches_endpoint)
    for match in matches:
        if match['comp_level'] == "qm":
            match_num = match['match_number']

            # Check if 'score_breakdown' exists and is not None
            if match.get('score_breakdown') is not None:
                red_rp = match['score_breakdown'].get('red', {}).get('rp', 0)
                blue_rp = match['score_breakdown'].get('blue', {}).get('rp', 0)
            else:
                red_rp = 0
                blue_rp = 0

            for team_id in match['alliances']['blue']['team_keys']:
                for i in range(len(team_rp_list)):
                    if team_rp_list[i][0] == team_id:
                        team_rp_list[i][1].append(blue_rp) # append this match's ranking point
                        team_rp_list[i][2].append(team_rp_list[i][3] + blue_rp) # add the raking points to the running total
                        team_rp_list[i][3] = team_rp_list[i][3] + blue_rp

            for team_id in match['alliances']['red']['team_keys']:
                for i in range(len(team_rp_list)):
                    if team_rp_list[i][0] == team_id:
                        team_rp_list[i][1].append(red_rp) # append this match's ranking point
                        team_rp_list[i][2].append(team_rp_list[i][3] + red_rp) # add the raking points to the running total
                        team_rp_list[i][3] = team_rp_list[i][3] + red_rp

    team_rp_list.sort(key=lambda x: x[3], reverse=True)
    for team in team_rp_list:
        log_msg(f"\nTEAM: {team[0]}", level=2)
        log_msg(f"\tRP by match: {team[1]}", level=2)
        log_msg(f"\tRP total by match: {team[2]}", level=2)
        log_msg(f"\tRP total: {team[3]}")
    return team_rp_list

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

    get_rp_per_match(event_endpoint=event_endpoint, team_num=team_endpoint)
    if VERBOSITY >= 2:
        get_event_matches(event_endpoint=event_endpoint, team_num=team_num)

if __name__ == "__main__":
    clear()
    main()