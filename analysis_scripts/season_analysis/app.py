from flask import Flask, render_template, request, jsonify
import plotly.graph_objects as go
import plotly.utils
import json
from tba_analysis.analysis_func import *

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')

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
    team_num = request.form.get('team_num')
    year = request.form.get('year')
    # year = get_event_year(request.form.get('event_key'))

    # Get penalty points data
    full_dist_pts_by_week = get_district_pts_by_week(get_team_district(team_num, year), year) # analyze Texas district

    # sort by team # before graphing
    full_dist_pts_by_week.sort(key=lambda x: x[0])

    # Ranking Points Graph
    dp_fig = go.Figure()
    for team_dp_by_week in full_dist_pts_by_week:
        week = []
        dp = []
        for event in team_dp_by_week[2]:
            week.append(event[0])
            dp.append(event[1])
        if team_dp_by_week[0] == team_num: # graph for the team being analyzed
            dp_fig.add_trace(
                go.Scatter(
                    x=week, # week number
                    y=dp, # district point total
                    mode='lines',
                    name=team_dp_by_week[0].replace('frc', ''),
                    line=dict(color='cyan', width=6, dash='solid')
                )
            )
        else: # not the team being analyzed
            dp_fig.add_trace(
                go.Scatter(
                    x=week, # week number
                    y=dp, # district point total
                    mode='lines',
                    name=team_dp_by_week[0].replace('frc', ''),
                    visible='legendonly',
                    line=dict(dash='dash')
                )
            )

    dp_fig.update_layout(
        title=f"District Point Breakdown - {get_team_districtname(team_num, year)}",
        xaxis_title="Week #",
        yaxis_title="District Ranking Points",
        template='plotly_dark',
        legend=dict(
            bgcolor='rgba(0,0,0,0.7)',   # semi-transparent dark bg
            font=dict(color='white'),   # white text
            bordercolor='white',
            borderwidth=1
        )
    )

    # sort by district points before making chart
    full_dist_pts_by_week.sort(key=lambda x: x[1], reverse=True)

    # Prepare the response data
    response_data = {
        'dp_graph': json.dumps(dp_fig, cls=plotly.utils.PlotlyJSONEncoder),
        'total_rankings': [{
            'team': item[0].replace('frc', ''),
            'score': item[1]
        } for item in full_dist_pts_by_week],
    }

    return jsonify(response_data)

if __name__ == '__main__':
    app.run(debug=True)