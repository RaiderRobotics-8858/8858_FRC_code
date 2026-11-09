"""Local, read-only MCP server for common The Blue Alliance queries."""

import json
import re
from typing import Any

from mcp.server.fastmcp import FastMCP

from tba_analysis.analysis_func import BASE_URL, fetch_tba_json, set_verbosity


mcp = FastMCP("TBA FRC Information")
set_verbosity(0)

_TEAM_NUMBER = re.compile(r"^[0-9]{1,6}$")
_YEAR = re.compile(r"^[0-9]{4}$")
_EVENT_KEY = re.compile(r"^[a-z0-9][a-z0-9_-]*$")


def _team_number(team_number: str | int) -> str:
    value = str(team_number).strip()
    if not _TEAM_NUMBER.fullmatch(value):
        raise ValueError("team_number must be a numeric FRC team number")
    return value


def _year(year: str | int) -> str:
    value = str(year).strip()
    if not _YEAR.fullmatch(value) or not 1900 <= int(value) <= 2200:
        raise ValueError("year must be a four-digit year")
    return value


def _event_key(event_key: str) -> str:
    value = event_key.strip().lower()
    if not _EVENT_KEY.fullmatch(value):
        raise ValueError("event_key contains unsupported characters")
    return value


def _json(data: Any) -> str:
    return json.dumps(data, indent=2, sort_keys=True)


@mcp.tool()
def get_team_info(team_number: str | int) -> str:
    """Return basic information about an FRC team."""
    team = _team_number(team_number)
    data = fetch_tba_json(f"{BASE_URL}/team/frc{team}")
    return _json({
        "team_number": team,
        "name": data.get("nickname"),
        "full_name": data.get("name"),
        "city": data.get("city"),
        "state": data.get("state_prov"),
        "country": data.get("country"),
        "rookie_year": data.get("rookie_year"),
        "website": data.get("website"),
        "tba_url": f"https://www.thebluealliance.com/team/{team}",
    })


@mcp.tool()
def get_team_events(team_number: str | int, year: str | int) -> str:
    """Return the events attended by a team in a given year."""
    team = _team_number(team_number)
    season = _year(year)
    data = fetch_tba_json(f"{BASE_URL}/team/frc{team}/events/{season}")
    return _json([
        {
            "key": event.get("key"),
            "name": event.get("name"),
            "city": event.get("city"),
            "state": event.get("state_prov"),
            "start_date": event.get("start_date"),
            "end_date": event.get("end_date"),
            "event_type": event.get("event_type_string"),
        }
        for event in data
    ])


@mcp.tool()
def get_event_info(event_key: str) -> str:
    """Return details for a TBA event key such as 2026code."""
    event = _event_key(event_key)
    data = fetch_tba_json(f"{BASE_URL}/event/{event}")
    return _json(data)


@mcp.tool()
def get_event_teams(event_key: str) -> str:
    """Return teams registered at an event."""
    event = _event_key(event_key)
    data = fetch_tba_json(f"{BASE_URL}/event/{event}/teams")
    return _json([
        {
            "team_number": team.get("key", "").removeprefix("frc"),
            "name": team.get("nickname"),
            "full_name": team.get("name"),
            "city": team.get("city"),
            "state": team.get("state_prov"),
        }
        for team in data
    ])


@mcp.tool()
def get_event_matches(event_key: str, team_number: str | int | None = None) -> str:
    """Return event matches, optionally filtered to one team."""
    event = _event_key(event_key)
    team_key = f"frc{_team_number(team_number)}" if team_number is not None else None
    matches = fetch_tba_json(f"{BASE_URL}/event/{event}/matches")
    if team_key is not None:
        matches = [
            match for match in matches
            if team_key in match.get("alliances", {}).get("red", {}).get("team_keys", [])
            or team_key in match.get("alliances", {}).get("blue", {}).get("team_keys", [])
        ]
    return _json(matches)


@mcp.tool()
def get_event_rankings(event_key: str) -> str:
    """Return the current TBA rankings for an event."""
    event = _event_key(event_key)
    data = fetch_tba_json(f"{BASE_URL}/event/{event}/rankings")
    return _json(data)


@mcp.tool()
def get_match_rank_deltas(event_key: str, match_number: int, match_type: str = "qm") -> str:
    """Return estimated ranking changes caused by a completed match."""
    from tba_analysis.analysis_func import get_match_rank_deltas as calculate_deltas

    event = _event_key(event_key)
    if match_number < 1:
        raise ValueError("match_number must be positive")
    if match_type not in {"qm", "sf", "f"}:
        raise ValueError("match_type must be qm, sf, or f")
    return _json(calculate_deltas(f"{BASE_URL}/event/{event}", match_number, match_type))


if __name__ == "__main__":
    mcp.run(transport="stdio")