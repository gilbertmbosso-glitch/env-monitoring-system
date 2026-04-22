import json
import random
import sys


def point_prediction(payload):
    pollution = float(payload.get("pollution", 0))
    temperature = float(payload.get("temperature", 20))
    humidity = float(payload.get("humidity", 50))

    temp_effect = (temperature - 20) * 0.6
    humidity_effect = (50 - humidity) * 0.3
    prediction = pollution + temp_effect + humidity_effect + random.uniform(-5, 5)

    return {"prediction": round(max(0, prediction), 2)}


def forecast_prediction(payload):
    raw_series = payload.get("series", [])
    series = [float(value) for value in raw_series if value is not None]
    horizon = max(1, min(int(payload.get("horizon", 6)), 12))

    if not series:
        forecast = [50.0 for _ in range(horizon)]
        return {"forecast": forecast}

    if len(series) == 1:
        forecast = [round(max(0, series[0]), 2) for _ in range(horizon)]
        return {"forecast": forecast}

    recent_window = series[-6:]
    average_level = sum(recent_window) / len(recent_window)

    deltas = []
    for index in range(1, len(recent_window)):
        deltas.append(recent_window[index] - recent_window[index - 1])

    trend = sum(deltas) / len(deltas) if deltas else 0
    trend *= 0.65

    last_value = recent_window[-1]
    forecast = []
    for step in range(1, horizon + 1):
        smoothed = (last_value * 0.7) + (average_level * 0.3)
        value = smoothed + trend * step + random.uniform(-2, 2)
        forecast.append(round(max(0, value), 2))

    return {"forecast": forecast}


def main():
    raw = sys.argv[1]
    payload = json.loads(raw)
    mode = payload.get("mode", "point")

    if mode == "forecast":
        print(json.dumps(forecast_prediction(payload)))
        return

    print(json.dumps(point_prediction(payload)))


if __name__ == "__main__":
    try:
        main()
    except Exception:
        print(json.dumps({"prediction": 0, "forecast": []}))
