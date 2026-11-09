
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

def main():

    parser = argparse.ArgumentParser(
        formatter_class=CustomHelpFormatter,
        description=help_text
    )

    parser.add_argument("-t", "-team_num", dest = "team_num", type = str, default=["prompt in script"], nargs = 1, help="Team Number to analyze")
    parser.add_argument("-y", "-year", dest = "year", type = str, default=["prompt in script"], nargs = 1, help="Year to fetch events from")
    parser.add_argument("-v", "--verbosity", dest = "verbosity", type = int, default = 1, choices = [0, 1, 2], help="0=quiet, 1=normal (default), 2=verbose/debug")
    parser.add_argument("-d", "-debug_mode", dest = "debug_mode", action = 'store_true', help="[deprecated] alias for -v 2")

    # assign command line argument variables to their respective variables in the script
    args = parser.parse_args()
    team_num    = args.team_num[0]
    year        = args.year[0]
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

    # Check that a team exists
    validate_team(team_endpoint)

    full_dist_pts_by_week = get_district_pts_by_week(get_team_district(team_num, year), year) # analyze the full district

    log_msg(team_endpoint, level=2)
    log_msg(team_events, level=2)

if __name__ == "__main__":
    clear()
    main()