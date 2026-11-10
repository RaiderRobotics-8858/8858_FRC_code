"""
# Team-specific TBA data-fetching functions
"""

from tba_analysis.analysis_func import BASE_URL, fetch_tba_json, log_msg

__all__ = ["validate_team", "get_team_district", "get_team_name", "get_team_districtname"]


def validate_team(team_endpoint):
    """
    Sanity check to make sure the inputted team exists
    """
    team_data = fetch_tba_json(team_endpoint)
    log_msg(f"Team Name: {team_data['nickname']}")
    log_msg(f"Location: {team_data['city']}, {team_data['state_prov']}, {team_data['country']}")

def get_team_district(team_num, year):
    """
    Return the district code for a team in a given year
    """
    url = f"{BASE_URL}/team/frc{team_num}/districts"

    year = int(year)

    districts = fetch_tba_json(url)

    for d in districts:
        if year == d["year"]:
            log_msg(d, level=2)
            return d["abbreviation"]

    error_msg(f"Couldn't find district for {team_num} in year {year}")
    return "no_district_found"

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

    team_data = fetch_tba_json(url)

    log_msg(team_data['nickname'])
    return team_data['nickname']

def get_team_districtname(team_num, year):
    """
    Return the district code for a team in a given year
    """
    url = f"{BASE_URL}/team/frc{team_num}/districts"

    year = int(year)

    districts = fetch_tba_json(url)

    for d in districts:
        if year == d["year"]:
            log_msg(d, level=2)
            return d["display_name"]

    error_msg(f"Couldn't find district for {team_num} in year {year}")
    return "no_district_found"
