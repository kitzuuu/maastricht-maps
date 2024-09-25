package ui.uicontroller;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

/**
 * This class is responsible for adding a text formatter to a TextField.
 */
public class AddTextFormatter {

    /**
     * Adds a text formatter to the given TextField, this ensures that the input in
     * the TextField adheres to Maastrict's postcode format.
     *
     * @param textField The TextField to which the text formatter is to be added.
     */
    public static void addTextFormatter(TextField textField) {
        // Create a filter that checks if the input is correct
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            String input = change.getText();
            final int oldLength = change.getControlText().length();
            int newLength = change.getControlNewText().length();
            if (newLength < oldLength)
                return change;
            // Check if the input is correct
            switch (newLength) {
                case 1:
                    if (input.matches("6")) {
                        return change;
                    } else
                        return null;
                case 2:
                    if (input.matches("2")) {
                        return change;
                    } else
                        return null;
                case 3:
                    if (input.matches("[1-2]*")) {
                        return change;
                    } else
                        return null;
                case 4:
                    if (input.matches("[1-9]*")) {
                        return change;
                    } else
                        return null;
                case 5:
                case 6:
                    if (input.matches("[A-Z]*")) {
                        return change;
                    } else if (input.matches("[a-z]*")) {
                        change.setText(change.getText().toUpperCase());
                        return change;
                    } else {
                        return null;
                    }
                case 7:
                    return null;
            }
            return change;
        };
        // Add the filter to the text field
        textField.setTextFormatter(new TextFormatter<>(filter));
    }
}
