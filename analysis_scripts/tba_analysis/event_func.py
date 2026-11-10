"""
# Event-specific TBA data-fetching functions
"""

import tba_analysis.analysis_func as _core
from tba_analysis.analysis_func import BASE_URL, fetch_tba_json, log_msg

__all__ = ["get_event_name", "get_event_week", "get_event_year", "get_event_details", "get_event_matches", "count_event_dp"]


def get_event_name(event_key):
    event_endpoint = f"/event/{event_key}"
    event_data = fetch_tba_json(BASE_URL + event_endpoint)
    return event_data['name']

def get_event_week(event_key):
    event_endpoint = f"/event/{event_key}"
    event_data = fetch_tba_json(BASE_URL + event_endpoint)
    return event_data['week']

def get_event_year(event_key):
    event_endpoint = f"/event/{event_key}"
    event_data = fetch_tba_json(BASE_URL + event_endpoint)
    return event_data['year']

def get_event_details(event_key):
    """
    fetch event details
    """
    event_endpoint = f"/event/{event_key}"
    event_data = fetch_tba_json(BASE_URL + event_endpoint)
    log_msg(f"Event Name: {get_event_name(event_key)}")
    log_msg(f"Date: {event_data['start_date']} to {event_data['end_date']}")
    log_msg(f"Location: {event_data['city']}, {event_data['state_prov']}, {event_data['country']}\n")

# Function to fetch matches at the event
def get_event_matches(event_endpoint, team_num):
    """
    print a complete list of matches
    """
    matches_endpoint = f"{event_endpoint}/matches"
    team_key = f"frc{team_num}"
    matches = fetch_tba_json(matches_endpoint)
    log_msg(matches_endpoint, level=2)
    log_msg("\nMatches at the Event:", level=2)
    for match in matches:
        if team_key in match['alliances']['blue']['team_keys'] or team_key in match['alliances']['red']['team_keys']:
            log_msg(f"\nMatch {match['match_number']}: {match['alliances']}", level=2)
        else:
            log_msg(f"\nMatch {match['match_number']}: {match['alliances']}", level=2)

def count_event_dp(team_num: str, event_key: str):
    """
    Fetches the district points earned at a given event for a given team
    """
    url = f"{BASE_URL}/event/{event_key}/district_points"
    log_msg(url, level=2)
    if _core.VERBOSITY >= 2:
        get_event_details(event_key=event_key)

    week = 0
    team_points = 0
    qual_points = 0
    elim_points = 0
    alliance_points = 0
    award_points = 0

    data = fetch_tba_json(url)

    if not data or not isinstance(data, dict):
        try:
            week = get_event_week(event_key)
        except Exception:
            week = 0
        log_msg(f"No District Point information for {team_num} @ {get_event_name(event_key)}", level=2)
        return week, 0, 0, 0, 0, 0

    try:
        week = get_event_week(event_key)
        team_points = data['points'][f'frc{team_num}']['total']
        qual_points = data['points'][f'frc{team_num}']['qual_points']
        elim_points = data['points'][f'frc{team_num}']['elim_points']
        alliance_points = data['points'][f'frc{team_num}']['alliance_points']
        award_points = data['points'][f'frc{team_num}']['award_points']
        if team_points:
            log_msg(f"District points for {team_num} @ {get_event_name(event_key)} (week {week}): {team_points}")
        else:
            log_msg(f"No district points found for team {team_num} at event {get_event_name(event_key)}", level=2)
    except KeyError:
        log_msg(f"No District Point information for {team_num} @ {get_event_name(event_key)}", level=2)

    return week, qual_points, elim_points, alliance_points, award_points, team_points
