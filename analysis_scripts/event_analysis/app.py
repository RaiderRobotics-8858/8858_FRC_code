from flask import Flask, render_template, request, jsonify
import plotly.graph_objects as go
import plotly.utils
import json
import os
import threading
from tba_rank_by_pen_pts import get_pen_pts_ranks_by_team, BASE_URL, headers
from tba_analysis.analysis_func import *
from get_rp_per_match import get_rp_per_match

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')


@app.route('/shutdown', methods=['POST'])
def shutdown():
    threading.Timer(0.25, lambda: os._exit(0)).start()
    return jsonify({'success': True})


@app.route('/ping', methods=['GET'])
def ping():
    return jsonify({'success': True})


@app.route('/get_team_events', methods=['POST'])
def get_team_events():
    team_num = request.form.get('team_num')
    year = request.form.get('year')

    # Get team events for the year
    team_events_endpoint = f"{BASE_URL}/team/frc{team_num}/events/{year}"
    events = fetch_tba_json(team_events_endpoint)
    event_list = []
    for event in events:
        event_list.append({
            'key': event['key'],
            'name': event['name'],
            'start_date': event['start_date']
        })
    # Sort events by start date
    event_list.sort(key=lambda x: x['start_date'])
    return jsonify({'success': True, 'events': event_list})

@app.route('/analyze', methods=['POST'])
def analyze():
    event_key = request.form.get('event_key')
    team_num = request.form.get('team_num')
    year = request.form.get('year')
    event_endpoint = f"{BASE_URL}/event/{event_key}"

    # Get penalty points data
    total_ranked_list, total_ranking, diff_ranked_list, diff_ranking, match_breakdown = get_pen_pts_ranks_by_team(event_endpoint, team_num)
    team_rp_list = get_rp_per_match(event_endpoint, team_num)

    # Sort match breakdown by match number
    match_breakdown.sort(key=lambda x: x['match_num'])

    # Build match-by-match results and rank deltas for the selected team
    team_key = f"frc{team_num}"
    match_rankings = []
    for match_num, red_teams, red_score, blue_teams, blue_score in get_match_details(event_endpoint):
        if str(team_num) in red_teams:
            team_score, opp_score = red_score, blue_score
        elif str(team_num) in blue_teams:
            team_score, opp_score = blue_score, red_score
        else:
            continue

        if team_score > opp_score:
            result = 'W'
        elif team_score < opp_score:
            result = 'L'
        else:
            result = 'T'

        rank_deltas = get_match_rank_deltas(event_endpoint, match_num)
        team_delta = next((entry for entry in rank_deltas if entry['team'] == team_key), None)
        rank_delta_by_team = {entry['team']: entry for entry in rank_deltas}
        match_teams = []
        for alliance, alliance_teams in (('red', red_teams), ('blue', blue_teams)):
            for team in alliance_teams:
                team_rank = rank_delta_by_team.get(f"frc{team}")
                match_teams.append({
                    'team': team,
                    'alliance': alliance,
                    'pre_rank': team_rank['pre_rank'] if team_rank else None,
                })

        match_rankings.append({
            'match_num': match_num,
            'result': result,
            'team_score': team_score,
            'opp_score': opp_score,
            'match_teams': match_teams,
            'post_rank': team_delta['post_rank'] if team_delta else None,
            'delta': team_delta['delta'] if team_delta else None,
        })

    # Create bar chart for differentials
    teams = [item[0].replace('frc', '') for item in diff_ranked_list]
    differences = [item[1] for item in diff_ranked_list]

    pen_fig = go.Figure(data=[
        go.Bar(
            x=teams,
            y=differences,
            marker_color=['red' if diff > 0 else 'green' for diff in differences]
        )
    ])

    pen_fig.update_layout(
        title=f"Penalty Points Differential by Team - {get_event_name(event_key)} ({get_event_year(event_key)})",
        xaxis_title="Team Number",
        yaxis_title="Penalty Points Differential",
        template="plotly_dark"
    )

    # Ranking Points Graph
    rp_fig = go.Figure()
    for team in team_rp_list:

        # generate the X-axis
        matches = []
        for i in range(len(team[2])):
            matches.append(i + 1)

        if team[0] == f"frc{team_num}": # graph for the team being analyzed
            rp_fig.add_trace(
                go.Scatter(
                    x=matches,
                    y=team[2],
                    mode='lines',
                    name=team[0].replace('frc', ''),
                    line=dict(color='cyan', width=6, dash='solid')
                )
            )
        else: # not the team being analyzed
            rp_fig.add_trace(
                go.Scatter(
                    x=matches,
                    y=team[2],
                    mode='lines',
                    name=team[0].replace('frc', ''),
                    visible='legendonly',
                    line=dict(dash='dash')
                )
            )

    rp_fig.update_layout(
        title=f"Ranking Points by Match - {get_event_name(event_key)} ({get_event_year(event_key)})",
        xaxis_title="Match #",
        yaxis_title="Ranking Points",
        template='plotly_dark',
        legend=dict(
            bgcolor='rgba(0,0,0,0.7)',   # semi-transparent dark bg
            font=dict(color='white'),   # white text
            bordercolor='white',
            borderwidth=1
        )
    )

    # Prepare the response data
    response_data = {
        'pen_graph': json.dumps(pen_fig, cls=plotly.utils.PlotlyJSONEncoder),
        'rp_graph': json.dumps(rp_fig, cls=plotly.utils.PlotlyJSONEncoder),
        'total_rankings': [{
            'team': item[0].replace('frc', ''),
            'score': item[1]
        } for item in total_ranked_list],
        'diff_rankings': [{
            'team': item[0].replace('frc', ''),
            'diff': item[1]
        } for item in diff_ranked_list],
        'team_ranking': {
            'total': total_ranking,
            'differential': diff_ranking
        },
        'selected_team': team_num,
        'match_breakdown': match_breakdown,
        'match_rankings': match_rankings
    }

    return jsonify(response_data)

if __name__ == '__main__':
    app.run(debug=True)