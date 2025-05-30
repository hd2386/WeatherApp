
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import io.github.cdimascio.dotenv.Dotenv;

public class MainSceneController implements Initializable {

    private static final Dotenv dotenv = Dotenv.load();
    public static final String API_KEY = dotenv.get("API_KEY");

    @FXML
    private Button Sendbutton;

    @FXML
    private TextField latitudeField;

    @FXML
    private TextField longitudeField;

    @FXML
    private TableView<Parameter> Table;

    @FXML
    private TableColumn<Parameter, String> nameCol;

    @FXML
    private TableColumn<Parameter, String> valueCol;

    @FXML
    private Label Rspnslabel;

    @FXML
    private ComboBox<String> SelectBox;

    @FXML
    private TextArea Responsefield;

    @FXML
    private Label errLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Sendbutton.setDisable(true);

        if (latitudeField != null) {
            latitudeField.textProperty().addListener((observable, oldValue, newValue) -> {
                validateInputFields();
            });
        }

        if (longitudeField != null) {
            longitudeField.textProperty().addListener((observable, oldValue, newValue) -> {
                validateInputFields();
            });
        }
    }

    private void validateInputFields() {
        if (latitudeField == null || longitudeField == null) {
            Sendbutton.setDisable(true);
            return;
        }
        String lat = latitudeField.getText();
        String lon = longitudeField.getText();

        if (lat != null && !lat.trim().isEmpty() && lon != null && !lon.trim().isEmpty() &&
                lat.matches("-?\\d*(\\.\\d+)?") && lon.matches("-?\\d*(\\.\\d+)?")) {
            Sendbutton.setDisable(false);
        } else {
            Sendbutton.setDisable(true);
        }
    }

    @FXML
    public void btnsendClicked(ActionEvent event) throws JSONException {
        Responsefield.setEditable(false);
        errLabel.setVisible(false);
        errLabel.setText("");

        if (latitudeField == null || longitudeField == null) {
            errLabel.setVisible(true);
            errLabel.setText("Error: Latitude/Longitude fields not initialized.");
            errLabel.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        String latitudeStr = latitudeField.getText();
        String longitudeStr = longitudeField.getText();

        if (latitudeStr == null || latitudeStr.trim().isEmpty() ||
                longitudeStr == null || longitudeStr.trim().isEmpty()) {
            errLabel.setVisible(true);
            errLabel.setText("Error: Latitude and Longitude cannot be empty.");
            errLabel.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        try {
            Double.parseDouble(latitudeStr);
            Double.parseDouble(longitudeStr);
        } catch (NumberFormatException e) {
            errLabel.setVisible(true);
            errLabel.setText("Error: Latitude and Longitude must be valid numbers.");
            errLabel.setTextFill(javafx.scene.paint.Color.RED);
            Responsefield.setText("Error: " + e.getMessage());
            return;
        }

        String baseUrl = "https://api.openweathermap.org/data/3.0/onecall";
        String excludeParams = "minutely,hourly,daily,alerts";

        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            errLabel.setVisible(true);
            errLabel.setText("Error: API_KEY not loaded from .env file.");
            errLabel.setTextFill(javafx.scene.paint.Color.RED);
            Responsefield.setText("Error: API_KEY is missing. Check your .env file and configuration.");
            return;
        }

        String finalUrl = String.format("%s?lat=%s&lon=%s&exclude=%s&appid=%s",
                baseUrl, latitudeStr, longitudeStr, excludeParams, API_KEY);

        try {
            URL url = new URL(finalUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String responseBody = br.lines().collect(Collectors.joining());
            String formattedbody = formatJsonString(responseBody);
            Responsefield.setText(formattedbody);

            int responsecode = con.getResponseCode();

            if (responsecode >= 200 && responsecode < 300) {
                errLabel.setVisible(true);
                errLabel.setText("OK (" + responsecode + ")");
                errLabel.setTextFill(javafx.scene.paint.Color.GREEN);

                ObservableList<Parameter> data = FXCollections.observableArrayList();
                data.add(new Parameter("lat", latitudeStr));
                data.add(new Parameter("lon", longitudeStr));
                data.add(new Parameter("appid", "*******"));
                data.add(new Parameter("exclude", excludeParams));

                if (Table == null)
                    Table = new TableView<>();
                if (nameCol == null)
                    nameCol = new TableColumn<>("Name");
                if (valueCol == null)
                    valueCol = new TableColumn<>("Value");

                if (Table.getColumns().isEmpty()) {
                    nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
                    valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
                    Table.getColumns().addAll(nameCol, valueCol);
                }
                Table.setItems(data);

                // Clear fields after successful request
                latitudeField.setText("");
                longitudeField.setText("");

                // Update ComboBox only on successful response with valid JSON
                if (formattedbody != null && !formattedbody.trim().isEmpty()) {
                    updateComboBoxOptions(formattedbody, SelectBox);
                }
            } else {
                errLabel.setVisible(true);
                errLabel.setText("Error: " + responsecode);
                errLabel.setTextFill(javafx.scene.paint.Color.RED);
                Responsefield.setText(formattedbody);
            }

        } catch (IOException e) {
            Responsefield.setText("Error: " + e.getMessage());
            errLabel.setVisible(true);
            errLabel.setText("Connection/IO Error");
            errLabel.setTextFill(javafx.scene.paint.Color.RED);
        } catch (JSONException e) {
            Responsefield.setText("Error parsing JSON: " + e.getMessage());
            errLabel.setVisible(true);
            errLabel.setText("JSON Error");
            errLabel.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    private static String formatJsonString(String jsonString) throws JSONException {
        Object obj = new JSONTokener(jsonString).nextValue();
        if (obj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) obj;
            return jsonObject.toString(5);
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            return jsonArray.toString(5);
        } else {
            return null;
        }
    }

    private void updateComboBoxOptions(String jsonString, ComboBox<String> comboBox) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        SelectBox.setItems(findKeys(jsonObject));
        SelectBox.setOnAction(event -> {
            String selectedItem = SelectBox.getSelectionModel().getSelectedItem();
            try {
                if (selectedItem != null && !selectedItem.isEmpty()) {
                    Object selectedValue = getValueFromPath(jsonObject, selectedItem);
                    Responsefield.setText(selectedValue.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    public static ObservableList<String> findKeys(JSONObject obj) throws JSONException {
        ObservableList<String> keys = FXCollections.observableArrayList();
        Iterator iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            Object value = obj.get(key);
            if (value instanceof JSONObject) {
                ObservableList<String> childKeys = findKeys((JSONObject) value);
                for (String childKey : childKeys) {
                    keys.add(key + "." + childKey);
                }
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (int i = 0; i < array.length(); i++) {
                    Object arrayValue = array.get(i);
                    if (arrayValue instanceof JSONObject) {
                        ObservableList<String> childKeys = findKeys((JSONObject) arrayValue);
                        for (String childKey : childKeys) {
                            keys.add(key + "[" + i + "]." + childKey);
                        }
                    }
                }
            } else {
                keys.add(key);
            }
        }
        return keys;
    }

    public static Object getValueFromPath(JSONObject json, String path) throws JSONException {
        String[] parts = path.split("\\.");
        Object value = json;
        for (String part : parts) {
            if (value instanceof JSONObject) {
                value = ((JSONObject) value).get(part);
            } else if (value instanceof JSONArray) {
                int index = Integer.parseInt(part);
                value = ((JSONArray) value).get(index);
            }
        }
        return value;
    }

    public static List<Parameter> getParamsFromURL(String url) {
        List<Parameter> params = new ArrayList<>();
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] parts = pair.split("=");
                    if (parts.length == 2) {
                        String key = parts[0];
                        String value = parts[1];
                        params.add(new Parameter(key, value));
                    }
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return params;
    }
}