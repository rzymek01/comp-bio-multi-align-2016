package pg.eti.bio;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.biojava.nbio.alignment.template.SequencePair;
import org.biojava.nbio.alignment.template.SubstitutionMatrix;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.AmbiguityRNACompoundSet;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.compound.DNACompoundSet;
import org.biojava.nbio.core.sequence.compound.RNACompoundSet;
import org.biojava.nbio.core.sequence.template.CompoundSet;

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

import static javafx.scene.text.Font.font;

public class Controller implements Initializable {

    private static final ObservableList<String> typeItems = FXCollections.observableArrayList("DNA", "RNA", "Protein", "RNA (Protein)");
    private static final ObservableList<String> matrixItems = FXCollections.observableArrayList("nuc4_4", "blosum62", "pam250", "gonnet250", "JOND920103");
    private static final String GAP = "-"; // should be the same as in SimpleAlignedSequence.gap

    @FXML
    private HBox global1Box;
    @FXML
    private HBox global2Box;
    @FXML
    private Label globalLengthLabel;
    @FXML
    private Label globalDistanceLabel;
    @FXML
    private Label globalSimilarityLabel;
    @FXML
    private HBox local1Box;
    @FXML
    private HBox local2Box;
    @FXML
    private Label localLengthLabel;
    @FXML
    private Label localDistanceLabel;
    @FXML
    private Label localSimilarityLabel;
    @FXML
    private Button seq1LoadButton;
    @FXML
    private Button seq2LoadButton;
    @FXML
    private TextArea seq1TextArea;
    @FXML
    private TextArea seq2TextArea;
    @FXML
    private Label seq1LengthLabel;
    @FXML
    private Label seq2LengthLabel;
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
    private StringProperty seq2Length;
    private StringProperty seq1;
    private StringProperty seq2;
    private StringProperty localLength;
    private StringProperty localDistance;
    private StringProperty localSimilarity;
    private StringProperty globalLength;
    private StringProperty globalDistance;
    private StringProperty globalSimilarity;

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
        seq2Length = new SimpleStringProperty();
        seq1 = new SimpleStringProperty();
        seq2 = new SimpleStringProperty();
        localLength = new SimpleStringProperty();
        localDistance = new SimpleStringProperty();
        localSimilarity = new SimpleStringProperty();
        globalLength = new SimpleStringProperty();
        globalDistance = new SimpleStringProperty();
        globalSimilarity = new SimpleStringProperty();

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
        seq2LengthLabel.textProperty().bindBidirectional(seq2Length);
        seq1TextArea.textProperty().bindBidirectional(seq1);
        seq2TextArea.textProperty().bindBidirectional(seq2);
        localLengthLabel.textProperty().bindBidirectional(localLength);
        localDistanceLabel.textProperty().bindBidirectional(localDistance);
        localSimilarityLabel.textProperty().bindBidirectional(localSimilarity);
        globalLengthLabel.textProperty().bindBidirectional(globalLength);
        globalDistanceLabel.textProperty().bindBidirectional(globalDistance);
        globalSimilarityLabel.textProperty().bindBidirectional(globalSimilarity);
        seq1.addListener((observable, oldValue, newValue) -> seq1Length.setValue("Length: " + newValue.length()));
        seq2.addListener((observable, oldValue, newValue) -> seq2Length.setValue("Length: " + newValue.length()));

        seq1.setValue("");
        seq2.setValue("");
        seq1Length.setValue("Length: 0");
        seq2Length.setValue("Length: 0");

        substCostProperty.setValue(1);
        indelCostProperty.setValue(2);
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
            } else if (seq2LoadButton.equals(event.getSource())) {
                seq2.setValue(text);
            }
        }
    }

    public void onComputeButtonClicked(ActionEvent event) throws CompoundNotFoundException, FileNotFoundException {
        SubstitutionMatrix mat ;
        CompoundSet compoundSet;
        if (type.get().equals(typeItems.get(2)) || type.get().equals(typeItems.get(3))) {
            compoundSet = new AminoAcidCompoundSet();
        } else if (type.get().equals(typeItems.get(1))) {
            compoundSet = new RNACompoundSet();
        } else {
            compoundSet = new DNACompoundSet();
        }
        if ("".equals(matrixFile.get())) {
            File file = new File(matrixFile.get());
            mat = new SimpleSubstitutionMatrix(compoundSet, file);
        } else if (matrix.get().equals(matrixItems.get(0))) {
            mat = SubstitutionMatrixHelper.getNuc4_4();
        } else {
            mat = SubstitutionMatrixHelper.getAminoAcidSubstitutionMatrix(matrix.get());
        }
        GapPenalty localGapPenalty = new SimpleGapPenalty(openPenalty.get(), extendPenalty.get());
        GapPenalty globalGapPenalty = new SimpleGapPenalty(openPenalty.get(), extendPenalty.get());
        if (localGapPenalty.getOpenPenalty() == 0) {
            localGapPenalty.setOpenPenalty(1);
        }
        SequencePair spl = null;
        SequencePair spg = null;
        String queryLocalSeq = null;
        String targetLocalSeq = null;
        String queryGlobalSeq = null;
        String targetGlobalSeq = null;
        String s1 = seq1.get().replaceAll("\\s","");
        String s2 = seq2.get().replaceAll("\\s","");
        if (type.get().equals(typeItems.get(0))) {
            spl = computeDNAAlignment(s1, s2, Alignments.PairwiseSequenceAlignerType.LOCAL, localGapPenalty, mat);
            spg = computeDNAAlignment(s1, s2, Alignments.PairwiseSequenceAlignerType.GLOBAL, globalGapPenalty, mat);
        } else if (type.get().equals(typeItems.get(1))) {
            spl = computeRNAAlignment(s1, s2, Alignments.PairwiseSequenceAlignerType.LOCAL, localGapPenalty, mat);
            spg = computeRNAAlignment(s1, s2, Alignments.PairwiseSequenceAlignerType.GLOBAL, globalGapPenalty, mat);
        } else if (type.get().equals(typeItems.get(2))) {
            spl = computeProteinAlignment(s1, s2, Alignments.PairwiseSequenceAlignerType.LOCAL, localGapPenalty, mat);
            spg = computeProteinAlignment(s1, s2, Alignments.PairwiseSequenceAlignerType.GLOBAL, globalGapPenalty, mat);
        } else {
            //@todo
        }

        if (spl != null && spg != null) {
            queryLocalSeq = spl.getQuery().getSequenceAsString();
            targetLocalSeq = spl.getTarget().getSequenceAsString();
            queryGlobalSeq = spg.getQuery().getSequenceAsString();
            targetGlobalSeq = spg.getTarget().getSequenceAsString();
        }

        fillResult(queryLocalSeq, targetLocalSeq, localLength, localDistance, localSimilarity, local1Box, local2Box);
        fillResult(queryGlobalSeq, targetGlobalSeq, globalLength, globalDistance, globalSimilarity, global1Box, global2Box);
    }

    private void fillResult(String splq, String splt, StringProperty length, StringProperty dist, StringProperty simil, HBox box1, HBox box2) {
        int len = splq.length();
        int similarity = 0;
        int editDistance = 0;
        int substitutionCost = getSubstitutionCost();
        int indelCost = getIndelCost();

        length.setValue("Length: " + len);

        box1.getChildren().clear();
        box2.getChildren().clear();
        for (int i = 0; i < len; i++) {
            Color color = Color.YELLOW;
            Label l1 = new Label(String.valueOf(splq.charAt(i)));
            Label l2 = new Label(String.valueOf(splt.charAt(i)));
            l1.setFont(font("Monospaced"));
            l2.setFont(font("Monospaced"));
            if (splq.charAt(i) == splt.charAt(i)) {
                color = Color.GREEN;
                similarity += 1;
            } else if (splq.charAt(i) == GAP.charAt(0) || splt.charAt(i) == GAP.charAt(0)) {
                color = Color.RED;
                editDistance += indelCost;
            } else {
                editDistance += substitutionCost;
            }

            l1.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
            l2.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
            box1.getChildren().add(l1);
            box2.getChildren().add(l2);
        }

        dist.setValue("Edit distance: " + editDistance);
        simil.setValue("Similarity: " + similarity);
    }

    private SequencePair computeProteinAlignment(String s1, String s2, Alignments.PairwiseSequenceAlignerType t, GapPenalty gapPenalty, SubstitutionMatrix mat) throws CompoundNotFoundException {
        ProteinSequence query = new ProteinSequence(s1, AminoAcidCompoundSet.getAminoAcidCompoundSet());
        ProteinSequence target = new ProteinSequence(s2, AminoAcidCompoundSet.getAminoAcidCompoundSet());
        return Alignments.getPairwiseAlignment(query, target, t, gapPenalty, mat);
    }

    private SequencePair computeRNAAlignment(String s1, String s2, Alignments.PairwiseSequenceAlignerType t, GapPenalty gapPenalty, SubstitutionMatrix mat) throws CompoundNotFoundException {
        RNASequence query = new RNASequence(s1, AmbiguityRNACompoundSet.getRNACompoundSet());
        RNASequence target = new RNASequence(s2, AmbiguityRNACompoundSet.getRNACompoundSet());
        return Alignments.getPairwiseAlignment(query, target, t, gapPenalty, mat);
    }

    private SequencePair computeDNAAlignment(String s1, String s2, Alignments.PairwiseSequenceAlignerType t, GapPenalty penalty, SubstitutionMatrix subMatrix) throws CompoundNotFoundException {
        DNASequence query = new DNASequence(s1, AmbiguityDNACompoundSet.getDNACompoundSet());
        DNASequence target = new DNASequence(s2, AmbiguityDNACompoundSet.getDNACompoundSet());
        return Alignments.getPairwiseAlignment(query, target, t, penalty, subMatrix);
    }

    private int getSubstitutionCost() {
        return substCostProperty.get();
    }

    private int getIndelCost() {
        return indelCostProperty.get();
    }
}
