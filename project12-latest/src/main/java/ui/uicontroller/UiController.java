package ui.uicontroller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gluonhq.maps.MapView;

import apiinteraction.ApiCaller;
import dataprocessing.CheckForPostalCodeInExcel;
import dataprocessing.ExcelReader;
import dataprocessing.ExcelWriter;
import datatypes.JourneyData;
import datatypes.MapListData;
import datatypes.Stop;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import jfxtras.scene.control.LocalTimePicker;
import maphandlers.CheckPostalCodesFormat;
import maphandlers.DatabaseHandler;
import maphandlers.ShortestPath;

public class UiController {

    public ShortestPath sp;
    public BorderPane borderPane;
    public LocalTimePicker timePicker;
    public TextField postCodeField1;
    public TextField postCodeField2;
    public StackPane mapStackPane;
    public Label walkTimeAndFirstStopLabel;
    public Label RouteDetailsLabel;
    public Label transferYesOrNoLabel;
    public Label transferAtStopsLabel;
    public Label totalJourneyTimeLabel;
    public TitledPane routeDetailsPane;
    public TitledPane transferDetailsPane;
    public Label arrivalTimeLabel;
    public VBox routeDetailsBox;
    public VBox transferDetailsBox;

    HashMap<String, String> hashMap;
    List<String> keySorted;
    private boolean isFirstTime = true;
    MapView mapView = MapViewer.createMapView();
    static DatabaseHandler dbHandler = new DatabaseHandler();
    private final double area = 1000; // max distance user can walk to a bus-stop

    public void initialize() {
        try {
            MapListData mapListData = ExcelReader.read();
            hashMap = mapListData.getMap();
            keySorted = mapListData.getKeySorted();
        } catch (Exception e) {
            System.out.println("Error reading postcode excel file");
        }
        mapStackPane.getChildren().add(mapView);
        addTextFormatter();
    }

    private void addTextFormatter() {
        AddTextFormatter.addTextFormatter(postCodeField1);
        AddTextFormatter.addTextFormatter(postCodeField2);
    }

    public static double[] calculateMidpoint(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate the midpoint coordinates
        double dLon = lon2Rad - lon1Rad;

        double Bx = Math.cos(lat2Rad) * Math.cos(dLon);
        double By = Math.cos(lat2Rad) * Math.sin(dLon);

        double lat3 = Math.atan2(Math.sin(lat1Rad) + Math.sin(lat2Rad),
                Math.sqrt((Math.cos(lat1Rad) + Bx) * (Math.cos(lat1Rad) + Bx) + By * By));
        double lon3 = lon1Rad + Math.atan2(By, Math.cos(lat1Rad) + Bx);

        // Convert the midpoint coordinates from radians back to degrees
        double midLat = Math.toDegrees(lat3);
        double midLon = Math.toDegrees(lon3);

        return new double[]{midLat, midLon};
    }

    @FXML
    public void calculate() throws IOException, SQLException {
        resetLabels();
        if (!isFirstTime) {
            mapStackPane.getChildren().clear();
            mapView = MapViewer.createMapView();
            mapStackPane.getChildren().add(mapView);
        }
        isFirstTime = false;
        LocalTime localTime = timePicker.getLocalTime();
        String postCode1 = postCodeField1.getText();
        String postCode2 = postCodeField2.getText();
        String[] postCode1Coordinates = new String[2];
        String[] postCode2Coordinates = new String[2];
        boolean firstInFile = checkPostalCodes(postCode1);
        boolean secondInFile = checkPostalCodes(postCode2);
        if (CheckPostalCodesFormat.check(postCodeField1, postCodeField2)) {
            // Check if the postal codes are in the Excel file
            if (firstInFile) {
                postCode1Coordinates = hashMap.get(postCode1).split(",");
            } else {
                // Get the coordinates of the first postal code
                try {
                    postCode1Coordinates = ApiCaller.getCoordinates(postCode1);
                    if (ApiCaller.error.isEmpty()) {
                        hashMap.put(postCode1, String.format("%s,%s", postCode1Coordinates[0], postCode1Coordinates[1]));
                        keySorted.add(postCode1);
                        Collections.sort(keySorted);
                    }
                } catch (Exception _) {
                }
            }
            // Check if the postal codes are in the Excel file
            if (secondInFile) {
                postCode2Coordinates = hashMap.get(postCode2).split(",");
            } else if (!firstInFile) {
                for (int i = 11; i > 0; --i) {
                    try {
                        sleep();
                    } catch (Exception _) {
                    }
                }
                try {
                    // Get the coordinates of the second postal code
                    postCode2Coordinates = ApiCaller.getCoordinates(postCode2);
                    if (ApiCaller.error.isEmpty()) {
                        hashMap.put(postCode2, String.format("%s,%s", postCode2Coordinates[0], postCode2Coordinates[1]));
                        keySorted.add(postCode1);
                        Collections.sort(keySorted);
                    }
                } catch (Exception _) {
                }

            }
            ExcelWriter.write(hashMap);

            ArrayList<Stop> stopsNearStart = new ArrayList<>();
            ArrayList<Stop> stopsNearEnd = new ArrayList<>();
            DatabaseHandler.checkStopsInArea(stopsNearEnd, stopsNearStart, postCode1Coordinates[0], postCode1Coordinates[1], postCode2Coordinates[0], postCode2Coordinates[1], area);

            // DIJKSTRA GRAPH ATTEMPT
            sp = new ShortestPath();

            boolean samePost = false;

            if(postCode1Coordinates[0].equals(postCode2Coordinates[0]) && postCode1Coordinates[1].equals(postCode2Coordinates[1])){
                samePost = true;
            }
            else{
                double pc1lat = Double.parseDouble(postCode1Coordinates[0]);
                double pc1lon = Double.parseDouble(postCode1Coordinates[1]);
                double pc2lat = Double.parseDouble(postCode2Coordinates[0]);
                double pc2lon = Double.parseDouble(postCode2Coordinates[1]);
    
                double[] midpoint = calculateMidpoint(pc1lat, pc1lon, pc2lat, pc2lon);
    
                sp.GetPath(stopsNearStart, stopsNearEnd,
                        calculations.aerialdistance.AerialDistance.calculateDistance(pc1lat, pc1lon, pc2lat, pc2lon),
                        timePicker.getLocalTime(), midpoint, postCode1Coordinates, postCode2Coordinates);
    
            }

            JourneyData journeyData = sp.getJourneyData();

            boolean walkroute = sp.drawRoute(mapView, postCode1Coordinates, postCode2Coordinates, journeyData);
          
            if(journeyData == null){
                if(samePost)
                walkTimeAndFirstStopLabel.setText("Please enter 2 different postal codes");
                
                walkTimeAndFirstStopLabel.setText("No route found, sorry.");
                return;
            }
            else if(walkroute){
                walkTimeAndFirstStopLabel.setText("Fastest route is by walking : " + sp.getTotalDuration() + " minutes");
            }
            else{
                updateLabels(journeyData);
            }
            
            
        }
    }

    private void updateLabels(JourneyData journeyData) {
        // Update the initial walk time and first stop label
        walkTimeAndFirstStopLabel.setText("Walk " + journeyData.getInitialWalkTime() + " minutes to " + journeyData.getBusRoutes().entrySet().iterator().next().getKey());
        totalJourneyTimeLabel.setText("Total Journey Time: " + journeyData.getTotalJourneyTime() + " minutes");

        if (!journeyData.getBusRoutes().isEmpty()) {
            StringBuilder routesDescription = new StringBuilder("Bus Routes:\n");

            // Get bus numbers and stops together
            ArrayList<String> busNumbers = journeyData.getBusNumbers();
            LinkedHashMap<String, Integer> busRoutes = journeyData.getBusRoutes();
            int index = 0;

            for (Map.Entry<String, Integer> entry : busRoutes.entrySet()) {
                String busNumber = busNumbers.size() > index ? busNumbers.get(index) : "Unknown";
                routesDescription.append("Bus ").append(busNumber).append(" - ").append(entry.getKey()).append(" - ").append(entry.getValue()).append(" mins\n");
                index++;
            }

            RouteDetailsLabel.setText(routesDescription.toString());
            routeDetailsBox.getChildren().clear();
            routeDetailsBox.getChildren().add(new Label(routesDescription.toString()));
        } else {
            RouteDetailsLabel.setText("No bus routes available.");
        }

        transferYesOrNoLabel.setText(journeyData.hasTransfers() ? "Transfers?: Yes" : "Transfers?: No");
        if (journeyData.hasTransfers() && !journeyData.getTransferDetails().isEmpty()) {
            StringBuilder transferDetails = new StringBuilder("Transfer Details:\n");
            for (String detail : journeyData.getTransferDetails()) {
                transferDetails.append(detail).append("\n");
            }
            transferAtStopsLabel.setText(transferDetails.toString());
            transferDetailsBox.getChildren().clear();
            transferDetailsBox.getChildren().add(new Label(transferDetails.toString()));
        } else {
            transferAtStopsLabel.setText("No transfers.");
        }

        // Update arrival time label
        arrivalTimeLabel.setText("Arrival Time: " + journeyData.getArrivalTime().toString());
    }



    private boolean checkPostalCodes(String postCode) {
        return CheckForPostalCodeInExcel.search(keySorted, postCode);
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(1000);
    }

    private void resetLabels() {
        walkTimeAndFirstStopLabel.setText("Preparing...");
        RouteDetailsLabel.setText("");
        transferYesOrNoLabel.setText("");
        transferAtStopsLabel.setText("");
        totalJourneyTimeLabel.setText("");
        routeDetailsBox.getChildren().clear();
        routeDetailsBox.getChildren().add(new Label("Route details will be displayed here."));
        transferDetailsBox.getChildren().clear();
        transferDetailsBox.getChildren().add(new Label("Transfer details will be displayed here."));
    }
}
