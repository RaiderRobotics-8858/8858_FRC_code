"""
# Season/district-level TBA data-fetching functions
"""

import datetime
import json
from pathlib import Path

from tba_analysis.analysis_func import BASE_URL, CACHE_DIR, fetch_tba_json, log_msg
from tba_analysis.event_func import get_event_details, count_event_dp

__all__ = [
    "get_event_id_list",
    "count_team_dp",
    "get_district_pts_by_week",
    "get_district_event_list",
    "download_district_events",
]


def get_event_id_list(team_events_endpoint, evnum):
    """
    get list of events a team participated in for a given year
    """
    event_ids = []

    team_event_list = fetch_tba_json(team_events_endpoint)
    i = 1
    for event in team_event_list:
        event_ids.append(event['key'])
        if evnum == "prompt in script":
            log_msg(f"\nEVENT #{i}:")
            get_event_details(event['key'])
        i = i + 1

    if evnum == "prompt in script":
        evnum = input(f"\nSELECT WHICH EVENT OF THE ABOVE {i - 1} CHOICES YOU'D LIKE TO ANALYZE: ")

    return event_ids, evnum

def count_team_dp(team_num, team_events:str):
    """
    Calculates the # of district points for a given team
    """
    event_ids = []
    district_pts_by_week = []
    district_pts = 0

    data = fetch_tba_json(team_events)

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

    return district_pts, district_pts_by_week

def get_district_pts_by_week(district_key, year):
    """
    Fetches a full district's points by week
    """
    teamlist_url = f"{BASE_URL}/district/{year}{district_key}/teams"
    full_dist_pts_by_week = []

    teams = fetch_tba_json(teamlist_url)
    for team in teams:
        team_num = team['key'].replace("frc", "")
        team_endpoint = f"{BASE_URL}/team/frc{team_num}"
        team_events = f"{team_endpoint}/events/{year}"
        district_pts, district_pts_by_week = count_team_dp(team_num, team_events)
        full_dist_pts_by_week.append([team_num, district_pts, district_pts_by_week])

    return full_dist_pts_by_week


def get_district_event_list(district_key, year, force_refresh=False):
    """
    Return every event in a district for a given year.
    """
    district_events_url = f"{BASE_URL}/district/{year}{district_key}/events"
    return fetch_tba_json(district_events_url, force_refresh=force_refresh)


def download_district_events(district_key, year, output_dir=None, force_refresh=False):
    """
    Download and cache every event in a district for a given year.

    Each event is written as a separate JSON file, along with its matches list,
    and the district event list is saved alongside them so the export can be
    reused offline.
    """
    district_events = get_district_event_list(district_key, year, force_refresh=force_refresh)

    if output_dir is None:
        output_dir = CACHE_DIR / "exports" / f"{year}{district_key}"

    export_dir = Path(output_dir)
    export_dir.mkdir(parents=True, exist_ok=True)

    district_index_path = export_dir / f"district_{year}{district_key}_events.json"
    with district_index_path.open("w", encoding="utf-8") as district_file:
        json.dump(district_events, district_file, indent=2)

    saved_files = [district_index_path]

    for event in district_events:
        event_key = event["key"]
        event_url = f"{BASE_URL}/event/{event_key}"
        event_data = fetch_tba_json(event_url, force_refresh=force_refresh)
        event_path = export_dir / f"{event_key}.json"
        with event_path.open("w", encoding="utf-8") as event_file:
            json.dump(event_data, event_file, indent=2)
        saved_files.append(event_path)

        matches_url = f"{event_url}/matches"
        matches_data = fetch_tba_json(matches_url, force_refresh=force_refresh)
        matches_path = export_dir / f"{event_key}_matches.json"
        with matches_path.open("w", encoding="utf-8") as matches_file:
            json.dump(matches_data, matches_file, indent=2)
        saved_files.append(matches_path)

    return district_events, saved_files
