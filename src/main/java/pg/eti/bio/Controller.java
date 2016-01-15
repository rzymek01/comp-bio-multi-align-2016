package pg.eti.bio;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.biojava.nbio.alignment.Alignments;
import org.biojava.nbio.alignment.SimpleAlignedSequence;
import org.biojava.nbio.alignment.SimpleGapPenalty;
import org.biojava.nbio.alignment.SimpleSequencePair;
import org.biojava.nbio.alignment.SimpleSubstitutionMatrix;
import org.biojava.nbio.alignment.SubstitutionMatrixHelper;
import org.biojava.nbio.alignment.template.AlignedSequence;
import org.biojava.nbio.alignment.template.GapPenalty;
import org.biojava.nbio.alignment.template.Profile;
import org.biojava.nbio.alignment.template.SequencePair;
import org.biojava.nbio.alignment.template.SubstitutionMatrix;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.AmbiguityRNACompoundSet;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.compound.DNACompoundSet;
import org.biojava.nbio.core.sequence.compound.RNACompoundSet;
import org.biojava.nbio.core.sequence.template.CompoundSet;
import org.biojava.nbio.core.util.ConcurrencyTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pg.eti.bio.data.SampleData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static javafx.scene.text.Font.font;

public class Controller implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);
    private static final ObservableList<String> typeItems = FXCollections.observableArrayList("DNA", "RNA", "Protein", "RNA (Protein)");
    private static final ObservableList<String> matrixItems = FXCollections.observableArrayList("nuc4_4", "blosum62", "pam250", "gonnet250", "JOND920103");
    private static final String GAP = "-"; // should be the same as in SimpleAlignedSequence.gap

    @FXML
    private VBox inputVisualVBox;
    @FXML
    private VBox outputVisualVBox;
    @FXML
    private Label globalLengthLabel;
    @FXML
    private Label globalDistanceLabel;
    @FXML
    private Label globalSimilarityLabel;
    @FXML
    private Label localLengthLabel;
    @FXML
    private Label localDistanceLabel;
    @FXML
    private Label localSimilarityLabel;
    @FXML
    private Button seq1LoadButton;
    @FXML
    private TextArea seq1TextArea;
    @FXML
    private Label seq1LengthLabel;
    @FXML
    private TextField openPenaltyField;
    @FXML
    private TextField extendPenaltyField;
    @FXML
    private TextField substCostField;
    @FXML
    private TextField indelCostField;
    @FXML
    private TextField matrixFileField;
    @FXML
    private ChoiceBox<String> typeChoiceBox;
    @FXML
    private ChoiceBox<String> matrixChoiceBox;

    private IntegerProperty openPenalty;
    private IntegerProperty extendPenalty;
    private IntegerProperty substCostProperty;
    private IntegerProperty indelCostProperty;
    private StringProperty matrixFile;
    private StringProperty matrix;
    private StringProperty type;
    private StringProperty seq1Length;
    private StringProperty seq1;
    private StringProperty localLength;
    private StringProperty localDistance;
    private StringProperty localSimilarity;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openPenalty = new SimpleIntegerProperty();
        extendPenalty = new SimpleIntegerProperty();
        substCostProperty = new SimpleIntegerProperty();
        indelCostProperty = new SimpleIntegerProperty();
        matrixFile = new SimpleStringProperty();
        matrix = new SimpleStringProperty();
        type = new SimpleStringProperty();
        seq1Length = new SimpleStringProperty();
        seq1 = new SimpleStringProperty();
        localLength = new SimpleStringProperty();
        localDistance = new SimpleStringProperty();
        localSimilarity = new SimpleStringProperty();

        openPenaltyField.textProperty().bindBidirectional(openPenalty, new NumberStringConverter());
        extendPenaltyField.textProperty().bindBidirectional(extendPenalty, new NumberStringConverter());
        substCostField.textProperty().bindBidirectional(substCostProperty, new NumberStringConverter());
        indelCostField.textProperty().bindBidirectional(indelCostProperty, new NumberStringConverter());
        matrixFileField.textProperty().bindBidirectional(matrixFile);
        matrixChoiceBox.setItems(matrixItems);
        matrixChoiceBox.valueProperty().bindBidirectional(matrix);
        typeChoiceBox.setItems(typeItems);
        typeChoiceBox.valueProperty().bindBidirectional(type);
        type.addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(typeItems.get(2)) || newValue.equals(typeItems.get(3))) {
                matrix.setValue(matrixItems.get(1));
            } else {
                matrix.setValue(matrixItems.get(0));
            }
        });
        type.setValue(typeItems.get(0));
        seq1LengthLabel.textProperty().bindBidirectional(seq1Length);
        seq1TextArea.textProperty().bindBidirectional(seq1);
        localLengthLabel.textProperty().bindBidirectional(localLength);
        localDistanceLabel.textProperty().bindBidirectional(localDistance);
        localSimilarityLabel.textProperty().bindBidirectional(localSimilarity);

        seq1.addListener((observable, oldValue, newValue) -> seq1Length.setValue("Length: " + newValue.length()));

        seq1.setValue("");
        seq1Length.setValue("Length: 0");

        substCostProperty.setValue(1);
        indelCostProperty.setValue(2);

        //DEBUG
        SampleData samples = new SampleData();

        // simple multi alignment
        String input = samples.getAll();
        seq1.setValue(input);
    }

    public void onLoadButtonClicked(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        File file = fileChooser.showOpenDialog(((Node) event.getTarget()).getScene().getWindow());
        if (file != null) {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (text.matches("[AGCT\n]+")) {
                type.setValue(typeItems.get(0));
            } else if (text.matches("[AGCU\n]+")) {
                type.setValue(typeItems.get(1));
            } else {
                type.setValue(typeItems.get(2));
            }

            if (seq1LoadButton.equals(event.getSource())) {
                seq1.setValue(text);
            }
        }
    }

    //look at http://www.biojava.org/docs/api/org/biojava/nbio/alignment/package-summary.html
    public void onComputeButtonClicked(ActionEvent event) throws CompoundNotFoundException, FileNotFoundException {
        try {
            String input = seq1.get();
            List<String> chains = Lists.newArrayList(Splitter.on("\n").split(input))
                    .stream()
                    .filter(s -> !s.equals(""))
                    .collect(Collectors.toList());
            List<ProteinSequence> proteinSeqs = chains.stream()
                    .map(s -> {
                        try {
                            return new ProteinSequence(s);
                        } catch (CompoundNotFoundException e) {
                            log.warn("Unrecognized protein in chain");
                            return null;
                        }
                    })
                    .collect(Collectors.toList());

            Profile<ProteinSequence, AminoAcidCompound> profile = Alignments.getMultipleSequenceAlignment(proteinSeqs);
            final List<AlignedSequence<ProteinSequence, AminoAcidCompound>> alignedSequences = profile.getAlignedSequences();

            List<String> outputChains = alignedSequences.stream()
                    .map(s -> s.toString())
                    .collect(Collectors.toList());
            fillResult(chains, outputChains, inputVisualVBox, outputVisualVBox);
        } catch (Exception e) {
            log.warn("Something went wrong");
        }

    }

    private void fillBox(String seq, HBox box) {
        Color color = Color.WHITE;
        Label label = new Label(seq);
        label.setFont(font("Monospace"));
        label.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
        box.getChildren().add(label);
    }

    private void fillResult(List<String> inputs, List<String> outputs, VBox inputsBox, VBox outputsBox) {
        inputsBox.getChildren().clear();
        outputsBox.getChildren().clear();

        if (inputs.size() != outputs.size()) {
            log.warn("The number of inputs doesn't equal the number of outputs");
            return;
        }
        final int rows = inputs.size();

        for (int i = 0; i < rows; ++i) {
            HBox box1 = new HBox();
            fillBox(inputs.get(i), box1);
            inputsBox.getChildren().add(box1);

            HBox box2 = new HBox();
            fillBox(outputs.get(i), box2);
            outputsBox.getChildren().add(box2);
        }
    }

    private int getSubstitutionCost() {
        return substCostProperty.get();
    }

    private int getIndelCost() {
        return indelCostProperty.get();
    }
}
