"""
This script is intended to run on the Arduino driving the LED sign at our FRC pit
"""

from tba_analysis.analysis_func import *
import serial
import time

PORT = 'COM10'
BAUDRATE = 115200
TIMEOUT = 1

TEAM_NUM = 8858
YEAR = 2025

# Endpoint for team information
team_endpoint = f"{BASE_URL}/team/frc{TEAM_NUM}"
team_events = f"{team_endpoint}/events/{YEAR}"
validate_team(team_endpoint=team_endpoint)
evids, evnum = get_event_id_list(team_events, 0)
event_endpoint = f"{BASE_URL}/event/{evids[evnum - 1]}"

ser = serial.Serial(PORT, BAUDRATE, timeout=TIMEOUT)

def ser_cmd(input:str):
    ser.write((input + '\n').encode('utf-8'))
    time.sleep(0.5)
    if ser.in_waiting:
        response = ser.read(ser.in_waiting).decode('utf-8', errors='ignore')
        if("ERROR :" in response):
            print(f"Exiting script due to error from Arduino...\n{response}")
            exit()

def print_qual_matches():
    """
    prints qualification matches on the sign
    """
    match_list = get_match_details(event_endpoint=event_endpoint, match_type="qm")
    for matches in match_list:
        match_num = matches[0]
        red_teams = matches[1]
        red_score = matches[2]
        blue_teams = matches[3]
        blue_score = matches[4]
        ser_cmd(f"Match {match_num}:")
        if(red_score > blue_score):
            ser_cmd("color")
            ser_cmd("red")
            ser_cmd(f"RED WINS!")
        elif(blue_score > red_score):
            ser_cmd("color")
            ser_cmd("blue")
            ser_cmd(f"BLUE WINS!")
        else:
            ser_cmd("color")
            ser_cmd("purple")
            ser_cmd(f"TIE!")

        # print RED data
        ser_cmd("color")
        ser_cmd("red")
        ser_cmd(f"{red_teams[0]}, {red_teams[1]}, {red_teams[2]} - {red_score}")

        # print BLUE data
        ser_cmd("color")
        ser_cmd("blue")
        ser_cmd(f"{blue_score} - {blue_teams[0]}, {blue_teams[1]}, {blue_teams[2]}")

def main():
    ser_cmd("text")
    print_qual_matches()

if __name__ == "__main__":
    main()