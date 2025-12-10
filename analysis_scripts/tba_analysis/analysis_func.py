
"""
# Analysis Functions Library
"""

import requests
import os
import sys
import datetime
from pathlib import Path

dp_event_data = []

# Handle the API Key for The Blue Alliance
try:
    from tba_analysis.api_key import *

    if API_KEY == "your_api_key_here":
        print(f"ERROR : API_KEY does not look to be updated in {Path(__file__).resolve().parent}/api_key.py")
        print(f"\tGenerate an API Key from https://www.thebluealliance.com/account and paste it into this file")
        exit()

    # Headers for the request
    headers = {
        "X-TBA-Auth-Key": API_KEY
    }
except ImportError:
    print(f"An error occured while importing api_key.py, generating a blank copy to put your TBA API Key in")
    print(f"To generate an API Key, go to https://www.thebluealliance.com/account then add a key under \"Read API Keys\"")
    outfile = open(f"{Path(__file__).resolve().parent}/api_key.py", 'w')
    api_key_in = input("Enter your API Key here (leave blank to manually enter the key later):")
    if api_key_in == "":
        api_key_in = "your_api_key_here"
        print(f"INFO : API Key left blank to manually enter later to {Path(__file__).resolve().parent}/api_key.py")

    outfile.write(
f"""
\"\"\"Your Key for [The Blue Alliance's API](https://www.thebluealliance.com/apidocs)\"\"\"

API_KEY = \"{api_key_in}\"
"""
    )
    outfile.close()
    exit()

# Base URL for TBA API
BASE_URL = "https://www.thebluealliance.com/api/v3"

debug_mode = False

def clear():
    """
    function to clear the screen
    """
    if sys.platform == 'win32':
        _ = os.system('cls')
    else:
        _ = os.system('clear')

class color:
    """
    Class used to make it easier to print colors
    """
    GREY        = "\033[38;5;246m"
    GREEN       = "\033[38;5;10m"
    YELLOW      = "\033[38;5;220m"
    RED         = "\033[38;5;196m"
    WHITE       = "\033[38;5;255m"
    RESET       = "\033[0;0m"
    GREEN_BG    = "\033[48;5;10m\033[38;5;232m"  # bold black text on green background
    YELLOW_BG   = "\033[48;5;220m\033[38;5;232m" # bold black text on yellow background
    GREY_BG     = "\033[48;5;246m\033[38;5;232m" # bold black text on grey background
    RED_BG      = "\033[48;5;196m\033[38;5;232m"   # bold white text on red background

def validate_team(team_endpoint):
    """
    Sanity check to make sure the inputted team exists
    """
    response = requests.get(team_endpoint, headers=headers)

    if response.status_code == 200:
        team_data = response.json()
        print(f"Team Name: {team_data['nickname']}")
        print(f"Location: {team_data['city']}, {team_data['state_prov']}, {team_data['country']}")
    else:
        print(f"Failed to fetch data: {response.status_code} - {response.reason}")
        print("Unable to validate team, exiting...")
        exit()

def get_team_district(team_num, year):
    """
    Return the district code for a team in a given year
    """
    url = f"{BASE_URL}/team/frc{team_num}/districts"

    year = int(year)

    response = requests.get(url, headers=headers)
    districts = response.json()

    for d in districts:
        if year == d["year"]:
            print(d)
            return d["abbreviation"]

    print(f"ERROR : Couldn't find district for {team_num} in year {year}")
    return "no_district_found"

def get_match_details(event_endpoint : str, match_type = "qm"):
    matches_endpoint = f"{event_endpoint}/matches"
    response = requests.get(matches_endpoint, headers=headers)

    # contains a basic scoring breakdown for each match at the event
    match_list = []

    if response.status_code == 200:
        matches = response.json()
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


def get_team_name(team_num):
    """
    Returns the team's name

    examples:

    ```python
    # prints "Beast from the East"
    print(get_team_name(8858))
    # prints "Robonauts"
    print(get_team_name(118))
    ```
    """
    url = f"{BASE_URL}/team/frc{team_num}"

    response = requests.get(url, headers=headers)
    team_data = response.json()

    print(team_data['nickname'])
    return team_data['nickname']

def get_team_districtname(team_num, year):
    """
    Return the district code for a team in a given year
    """
    url = f"{BASE_URL}/team/frc{team_num}/districts"

    year = int(year)

    response = requests.get(url, headers=headers)
    districts = response.json()

    for d in districts:
        if year == d["year"]:
            print(d)
            return d["display_name"]

    print(f"ERROR : Couldn't find district for {team_num} in year {year}")
    return "no_district_found"

def get_event_id_list(team_events_endpoint, evnum):
    """
    get list of events a team participated in for a given year
    """
    event_ids = []

    response = requests.get(team_events_endpoint, headers=headers)

    if response.status_code == 200:
        team_event_list = response.json()
        i = 1
        for event in team_event_list:
            event_ids.append(event['key'])
            if evnum == "prompt in script":
                print(f"\nEVENT #{i}:")
                get_event_details(event['key'])
            i = i + 1
    else:
        print(f"Failed to fetch data: {response.status_code} - {response.reason}")
        print("Unable to detect event list for this team/year combination, exiting...")
        exit()

    if evnum == "prompt in script":
        evnum = input(f"\nSELECT WHICH EVENT OF THE ABOVE {i - 1} CHOICES YOU'D LIKE TO ANALYZE: ")

    return event_ids, evnum

def get_event_name(event_key):
    event_endpoint = f"/event/{event_key}"
    response = requests.get(BASE_URL + event_endpoint, headers=headers)
    if response.status_code == 200:
        event_data = response.json()
        return event_data['name']

def get_event_week(event_key):
    event_endpoint = f"/event/{event_key}"
    response = requests.get(BASE_URL + event_endpoint, headers=headers)
    if response.status_code == 200:
        event_data = response.json()
        return event_data['week']

def get_event_year(event_key):
    event_endpoint = f"/event/{event_key}"
    response = requests.get(BASE_URL + event_endpoint, headers=headers)
    if response.status_code == 200:
        event_data = response.json()
        return event_data['year']

def get_event_details(event_key):
    """
    fetch event details
    """
    event_endpoint = f"/event/{event_key}"
    response = requests.get(BASE_URL + event_endpoint, headers=headers)
    if response.status_code == 200:
        event_data = response.json()
        print(f"Event Name: {get_event_name(event_key)}")
        print(f"Date: {event_data['start_date']} to {event_data['end_date']}")
        print(f"Location: {event_data['city']}, {event_data['state_prov']}, {event_data['country']}\n")
    else:
        print(f"Failed to fetch event details: {response.status_code} - {response.reason}")
        exit()

# Function to fetch matches at the event
def get_event_matches(event_endpoint, team_num):
    """
    print a complete list of matches
    """
    matches_endpoint = f"{event_endpoint}/matches"
    response = requests.get(matches_endpoint, headers=headers)
    team_key = f"frc{team_num}"
    if response.status_code == 200:
        matches = response.json()
        if debug_mode:
            print(matches_endpoint)
            print("\nMatches at the Event:")
        for match in matches:
            if team_key in match['alliances']['blue']['team_keys'] or team_key in match['alliances']['red']['team_keys']:
                print(f"{color.GREEN}\nMatch {match['match_number']}: {match['alliances']}{color.RESET}")
            else:
                print(f"\nMatch {match['match_number']}: {match['alliances']}")
    else:
        print(f"Failed to fetch matches: {response.status_code} - {response.reason}")
        print(f"The event's matches list {matches_endpoint} could not be found, exiting...")
        exit()

def count_event_dp(team_num: str, event_key: str):
    """
    Fetches the district points earned at a given event for a given team
    """
    url = f"{BASE_URL}/event/{event_key}/district_points"
    if debug_mode:
        print(url)
        get_event_details(event_key=event_key)

    global dp_event_data

    week = 0
    team_points = 0
    qual_points = 0
    elim_points = 0
    alliance_points = 0
    award_points = 0

    found_flag = False

    for events in dp_event_data:
        if events[0] == url:
            found_flag = True
            week = events[1]

            try:
                team_points = events[2]['points'][f'frc{team_num}']['total']
                qual_points = events[2]['points'][f'frc{team_num}']['qual_points']
                elim_points = events[2]['points'][f'frc{team_num}']['elim_points']
                alliance_points = events[2]['points'][f'frc{team_num}']['alliance_points']
                award_points = events[2]['points'][f'frc{team_num}']['award_points']
                if team_points:
                    print(f"District points for {team_num} @ {get_event_name(event_key)} (week {week}): {team_points}")
                else:
                    print(f"No district points found for team {team_num} at event {get_event_name(event_key)}")
            except KeyError:
                print(f"INFO : No District Point information for {team_num} @ {get_event_name(event_key)}")

    # if this event hasn't already been saved to memory (via dp_event_data), save it here
    if not found_flag:
        response = requests.get(url, headers=headers)

        try:
            if response.status_code == 200:
                data = response.json()
                week = get_event_week(event_key)
                team_points = data['points'][f'frc{team_num}']['total']
                qual_points = data['points'][f'frc{team_num}']['qual_points']
                elim_points = data['points'][f'frc{team_num}']['elim_points']
                alliance_points = data['points'][f'frc{team_num}']['alliance_points']
                award_points = data['points'][f'frc{team_num}']['award_points']
                if team_points:
                    print(f"District points for {team_num} @ {get_event_name(event_key)} (week {week}): {team_points}")
                else:
                    print(f"No district points found for team {team_num} at event {get_event_name(event_key)}")

            else:
                print(f"Failed to fetch data: {response.status_code} - {response.reason}")
                print("Unable to detect event list for this team/year combination, exiting...")
                exit()
        except KeyError:
            print(f"INFO : No District Point information for {team_num} @ {get_event_name(event_key)}")

        # save this data in memory so it can be accessed quicker later!
        dp_event_data.append([url, week, data])

    return week, qual_points, elim_points, alliance_points, award_points, team_points

def count_team_dp(team_num, team_events:str):
    """
    Calculates the # of district points for a given team
    """
    event_ids = []
    district_pts_by_week = []
    district_pts = 0

    response = requests.get(team_events, headers=headers)

    if response.status_code == 200:
        data = response.json()

        # sort list by chronological order
        team_event_list = [
            e for e in data
        ]
        team_event_list.sort(key=lambda e: datetime.datetime.fromisoformat(e['start_date']))

        district_ev_cnt = 0
        for event in team_event_list:
            valid_event = True
            if event['event_type'] == 1:
                district_ev_cnt = district_ev_cnt + 1
                if district_ev_cnt > 2:
                    valid_event = False
            elif event['event_type'] == 0:
                valid_event = False

            event_key = event['key']
            week, qual_points, elim_points, alliance_points, award_points, team_points = count_event_dp(team_num, event_key)

            # only first two district events cout towards qualification for District Championship
            if valid_event:
                district_pts = district_pts + award_points
                district_pts = district_pts + qual_points
                district_pts = district_pts + elim_points
                district_pts = district_pts + alliance_points

            if(week != None):
                district_pts_by_week.append([week, district_pts])
    else:
        print(f"Failed to fetch data: {response.status_code} - {response.reason}")
        print("Unable to detect event list for this team/year combination, exiting...")
        exit()

    return district_pts, district_pts_by_week

def get_district_pts_by_week(district_key, year):
    """
    Fetches a full district's points by week
    """
    teamlist_url = f"{BASE_URL}/district/{year}{district_key}/teams"
    response = requests.get(teamlist_url, headers=headers)
    full_dist_pts_by_week = []

    if response.status_code == 200:
        teams = response.json()
        for team in teams:
            team_num = team['key'].replace("frc", "")
            team_endpoint = f"{BASE_URL}/team/frc{team_num}"
            team_events = f"{team_endpoint}/events/{year}"
            district_pts, district_pts_by_week = count_team_dp(team_num, team_events)
            full_dist_pts_by_week.append([team_num, district_pts, district_pts_by_week])

    return full_dist_pts_by_week
