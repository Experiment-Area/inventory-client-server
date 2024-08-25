package lk.ijse.dep12.client_1.controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import lk.ijse.dep12.shared.to.Item;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryController {

    public VBox vBox;
    @FXML
    private Button btnDelete;

    @FXML
    private Button btnNewItem;

    @FXML
    private Button btnSave;

    @FXML
    private TableView<Item> tblView;

    @FXML
    private TextField txtBarcode;

    @FXML
    private TextField txtDescription;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtQty;
    private Socket remoteSocket;
    private ObjectOutputStream oss;

    public void initialize() throws IOException, ClassNotFoundException {
        vBox.setDisable(true);

        remoteSocket = new Socket("localhost", 5050);
        oss = new ObjectOutputStream(remoteSocket.getOutputStream());
        loadStock();

        tblView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("barcode"));
        tblView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("description"));
        tblView.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("qty"));
        tblView.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("price"));

        tblView.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
            btnDelete.setDisable(current == null);
            btnSave.setDisable(current != null);

            if (current != null) {
                txtBarcode.setText(current.getBarcode());
                txtDescription.setText(current.getDescription());
                txtQty.setText(current.getQty() + "");
                txtPrice.setText(current.getPrice().toString());
            }
        });
    }

    private void loadStock() throws IOException, ClassNotFoundException {

        ObjectInputStream ois = new ObjectInputStream(remoteSocket.getInputStream());
        List<Item> itemList = (List<Item>) ois.readObject();
        tblView.getItems().addAll(itemList);
    }

    @FXML
    void btnDeleteOnAction(ActionEvent event) {

        btnDelete.setOnAction((e) -> {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType no = new ButtonType("NO", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are You Sure !", ok, no);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.orElse(no) == ok) {
                Item selectedItem = tblView.getSelectionModel().getSelectedItem();
                ObservableList<Item> tmList = tblView.getItems();
                tmList.remove(selectedItem);
                try {
                    oss.writeObject(new ArrayList<>(tmList));
                    loadStock();

                } catch (IOException | ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                if (tmList.isEmpty()) btnNewItem.fire();
            }
        });
    }

    @FXML
    void tblViewOnKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    @FXML
    void btnNewItemOnAction(ActionEvent event) {
        vBox.setDisable(false);
        btnDelete.setDisable(true);
        for (var txt : new TextField[]{txtBarcode, txtDescription, txtQty, txtPrice}) txt.clear();
        tblView.getSelectionModel().clearSelection();
        txtBarcode.requestFocus();
    }

    @FXML
    void btnSaveOnAction(ActionEvent event) throws IOException {

        TextField invalidFiled;
        if ((invalidFiled = validateData()) != null) {
            invalidFiled.requestFocus();
            invalidFiled.selectAll();
            return;
        }

        ObservableList<Item> itemsList = tblView.getItems();
        String barcode = txtBarcode.getText().strip();

        for (Item item : itemsList) {
            if (item.equals(barcode)) {
                new Alert(Alert.AlertType.ERROR, "Item is Already Added !").show();
            }
        }

        Item item = new Item(barcode,
                txtDescription.getText().strip(),
                Integer.parseInt(txtQty.getText()),
                new BigDecimal(txtPrice.getText()));
        itemsList.add(item);

        oss.writeObject(new ArrayList<>(itemsList));
        btnNewItem.fire();
    }

    private TextField validateData() {

        String barcode = txtBarcode.getText();
        String description = txtDescription.getText();
        String qty = txtQty.getText();
        String price = txtPrice.getText();

        if (barcode.isBlank() || !isNUmber(barcode.strip())) return txtBarcode;
        else if (description.isBlank() || description.length() < 3) return txtDescription;
        else if (qty.isBlank() || !isNUmber(qty.strip()) || Integer.parseInt(qty) <= 0) return txtQty;
        else if (price.isBlank() || !isPrice(price)) return txtPrice;

        return null;
    }

    private boolean isNUmber(String input) {
        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private boolean isPrice(String input) {
        try {
            BigDecimal price = new BigDecimal(input);
            return price.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException n) {
            return false;
        }
    }
}
