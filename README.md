# 🚌 Maastricht Journey Planner

A desktop journey planner for Maastricht, built by a team of seven for Project 1-2 of the BSc Computer Science at Maastricht University. Enter two Maastricht postcodes and a departure time, and it finds the fastest combination of walking and bus travel, then draws the journey on an interactive map.

## How it works

1. **Postcodes → coordinates:** postcodes are looked up in a bundled dataset, with a postcode web API as a fallback; new results are cached back into the dataset.
2. **Nearby stops:** bus stops within walking distance of the start and destination are selected from GTFS timetable data stored in MySQL. Walking times are estimated from the straight-line distance at 4.82 km/h.
3. **Routing:** stops, bus trips and walking links form a graph, and a **time-dependent Dijkstra** search finds the earliest arrival while respecting real departure times.
4. **Map:** the route, bus lines and stops are drawn on a Gluon Maps view with start and end pins.

The project also includes a GraphHopper API client for walking and cycling routes.

## Tech stack

Java 22 · JavaFX · Gluon Maps · MySQL (GTFS data) · OkHttp · Apache POI · Maven

**Testing:** JUnit 5 · Mockito · JMockit · WireMock · TestFX · JaCoCo

## Getting started

Prerequisites: JDK 22 (preview features enabled), Maven and MySQL.

1. Import `project12-latest/final dump.sql` into a MySQL database named `gtfs`.
2. Set the environment variables `DB_USER`, `DB_PASSWORD` and `GRAPHHOPPER_API_KEY` (free keys at [graphhopper.com](https://www.graphhopper.com/)).
3. From `project12-latest/`, run `mvn javafx:run`, or run `uistarter.Run` from your IDE.

Run the tests with a coverage report:

```bash
cd project12-latest
mvn clean test jacoco:report    # report: target/site/jacoco/index.html
```

## Team

M. Fares · B. Follinus · M. Hagenbeek · C. Niţu · E. Proctor · V. Shabanaj · L. Sivaprakash
