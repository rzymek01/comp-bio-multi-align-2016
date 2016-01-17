package pg.eti.bio;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.sun.deploy.util.StringUtils;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.biojava.nbio.alignment.Alignments;
import org.biojava.nbio.alignment.GuideTree;
import org.biojava.nbio.alignment.SimpleGapPenalty;
import org.biojava.nbio.alignment.SimpleProfile;
import org.biojava.nbio.alignment.SimpleSubstitutionMatrix;
import org.biojava.nbio.alignment.SubstitutionMatrixHelper;
import org.biojava.nbio.alignment.template.AlignedSequence;
import org.biojava.nbio.alignment.template.GapPenalty;
import org.biojava.nbio.alignment.template.PairwiseSequenceScorer;
import org.biojava.nbio.alignment.template.Profile;
import org.biojava.nbio.alignment.template.SubstitutionMatrix;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.template.CompoundSet;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javafx.scene.text.Font.font;

@SuppressWarnings("Duplicates")
public class Controller implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);
    private static final ObservableList<String> typeItems = FXCollections.observableArrayList("DNA", "Protein");
    private static final ObservableList<String> matrixItems = FXCollections.observableArrayList("nuc4_4", "blosum62", "pam250", "gonnet250", "JOND920103");
    private static final String GAP = "-"; // should be the same as in SimpleAlignedSequence.gap

    @FXML
    private Label profileMatrix1Label;
    @FXML
    private Label profileMatrix2Label;
    @FXML
    private Label profileMatrixLabel;
    @FXML
    private VBox input1VisualVBox;
    @FXML
    private VBox output1VisualVBox;
    @FXML
    private VBox outputVisualVBox;
    @FXML
    private VBox input2VisualVBox;
    @FXML
    private VBox output2VisualVBox;
    @FXML
    private Button computeButton;
    @FXML
    private Button compute1Button;
    @FXML
    private Button compute2Button;
    @FXML
    private Button seq1LoadButton;
    @FXML
    private Button seq2LoadButton;
    @FXML
    private TextArea seq1TextArea;
    @FXML
    private TextArea seq2TextArea;
    @FXML
    private TextField openPenaltyField;
    @FXML
    private TextField extendPenaltyField;
    @FXML
    private TextField matrixFileField;
    @FXML
    private ChoiceBox<String> typeChoiceBox;
    @FXML
    private ChoiceBox<String> matrixChoiceBox;

    private IntegerProperty openPenalty;
    private IntegerProperty extendPenalty;
    private StringProperty matrixFile;
    private StringProperty matrix;
    private StringProperty type;
    private StringProperty seq1;
    private StringProperty seq2;
    private StringProperty pm1;
    private StringProperty pm2;
    private StringProperty pm;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openPenalty = new SimpleIntegerProperty();
        extendPenalty = new SimpleIntegerProperty();
        matrixFile = new SimpleStringProperty();
        matrix = new SimpleStringProperty();
        type = new SimpleStringProperty();
        seq1 = new SimpleStringProperty();
        seq2 = new SimpleStringProperty();
        pm = new SimpleStringProperty();
        pm1 = new SimpleStringProperty();
        pm2 = new SimpleStringProperty();

        openPenaltyField.textProperty().bindBidirectional(openPenalty, new NumberStringConverter());
        extendPenaltyField.textProperty().bindBidirectional(extendPenalty, new NumberStringConverter());
        matrixFileField.textProperty().bindBidirectional(matrixFile);
        profileMatrix1Label.textProperty().bindBidirectional(pm1);
        profileMatrix2Label.textProperty().bindBidirectional(pm2);
        profileMatrixLabel.textProperty().bindBidirectional(pm);
        profileMatrix1Label.setFont(font("Monospace"));
        profileMatrix2Label.setFont(font("Monospace"));
        profileMatrixLabel.setFont(font("Monospace"));
        matrixChoiceBox.setItems(matrixItems);
        matrixChoiceBox.valueProperty().bindBidirectional(matrix);
        typeChoiceBox.setItems(typeItems);
        typeChoiceBox.valueProperty().bindBidirectional(type);
        type.addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(typeItems.get(1))) {
                matrix.setValue(matrixItems.get(1));
            } else {
                matrix.setValue(matrixItems.get(0));
            }
        });
        type.setValue(typeItems.get(1));
        seq1TextArea.textProperty().bindBidirectional(seq1);
        seq2TextArea.textProperty().bindBidirectional(seq2);

        seq1.setValue("");
        seq2.setValue("");
        matrixFile.setValue("");

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
            } else {
                type.setValue(typeItems.get(1));
            }

            if (seq1LoadButton.equals(event.getSource())) {
                seq1.setValue(text);
            } else if (seq2LoadButton.equals(event.getSource())) {
                seq2.setValue(text);
            }
        }
    }

    //look at http://www.biojava.org/docs/api/org/biojava/nbio/alignment/package-summary.html
    public void onComputeButtonClicked(ActionEvent event) throws CompoundNotFoundException, FileNotFoundException {
        SubstitutionMatrix mat ;
        CompoundSet compoundSet;
        if (type.get().equals(typeItems.get(1))) {
            compoundSet = new AminoAcidCompoundSet();
        } else {
            compoundSet = AmbiguityDNACompoundSet.getDNACompoundSet();
        }
        if (!"".equals(matrixFile.get())) {
            File file = new File(matrixFile.get());
            mat = new SimpleSubstitutionMatrix(compoundSet, file);
        } else if (matrix.get().equals(matrixItems.get(0))) {
            mat = SubstitutionMatrixHelper.getNuc4_4();
        } else {
            mat = SubstitutionMatrixHelper.getAminoAcidSubstitutionMatrix(matrix.get());
        }
        GapPenalty gapPenalty = new SimpleGapPenalty(openPenalty.get(), extendPenalty.get());
        String input = "";
        if (compute1Button.equals(event.getSource())) {
            input = seq1.get();
        } else if (compute2Button.equals(event.getSource())) {
            input = seq2.get();
        } else {
            input = seq1.get() + "\n" + seq2.get();
        }
        List<String> chains = Lists.newArrayList(Splitter.on("\n").split(input))
                .stream()
                .filter(s -> !s.equals(""))
                .collect(Collectors.toList());
        List<String> outputChains;
        if (type.get().equals(typeItems.get(0))) {
            if (computeButton.equals(event.getSource())) {
                SimpleProfile<DNASequence, NucleotideCompound> profile = computeProgressiveDNAAlignment(chains, gapPenalty, mat);
                List<AlignedSequence<DNASequence, NucleotideCompound>> alignedSequences = profile.getAlignedSequences();
                outputChains = alignedSequences.stream().map(Object::toString).collect(Collectors.toList());
                fillDNAProfileMatrix(pm, profile);
                fillResult(chains, outputChains, null, outputVisualVBox);
            } else {
                Profile<DNASequence, NucleotideCompound> profile = computeDNAAlignment(chains, gapPenalty, mat);
                List<AlignedSequence<DNASequence, NucleotideCompound>> alignedSequences = profile.getAlignedSequences();
                outputChains = alignedSequences.stream().map(Object::toString).collect(Collectors.toList());
                if (compute1Button.equals(event.getSource())) {
                    fillResult(chains, outputChains, input1VisualVBox, output1VisualVBox);
                    fillDNAProfileMatrix(pm1, profile);
                } else if (compute2Button.equals(event.getSource())) {
                    fillResult(chains, outputChains, input2VisualVBox, output2VisualVBox);
                    fillDNAProfileMatrix(pm2, profile);
                }
            }
        } else {
            if (computeButton.equals(event.getSource())) {
                SimpleProfile<ProteinSequence, AminoAcidCompound> profile = computeProgressiveProteinAlignment(chains, gapPenalty, mat);
                List<AlignedSequence<ProteinSequence, AminoAcidCompound>> alignedSequences = profile.getAlignedSequences();
                outputChains = alignedSequences.stream().map(Object::toString).collect(Collectors.toList());
                fillProteinProfileMatrix(pm, profile);
                fillResult(chains, outputChains, null, outputVisualVBox);
            } else {
                SimpleProfile<ProteinSequence, AminoAcidCompound> profile = computeProteinAlignment(chains, gapPenalty, mat);
                List<AlignedSequence<ProteinSequence, AminoAcidCompound>> alignedSequences = profile.getAlignedSequences();
                outputChains = alignedSequences.stream().map(Object::toString).collect(Collectors.toList());
                if (compute1Button.equals(event.getSource())) {
                    fillResult(chains, outputChains, input1VisualVBox, output1VisualVBox);
                    fillProteinProfileMatrix(pm1, profile);
                } else if (compute2Button.equals(event.getSource())) {
                    fillResult(chains, outputChains, input2VisualVBox, output2VisualVBox);
                    fillProteinProfileMatrix(pm2, profile);
                }
            }
        }
    }

    private SimpleProfile<ProteinSequence, AminoAcidCompound> computeProgressiveProteinAlignment(List<String> chains, GapPenalty gapPenalty, SubstitutionMatrix mat) {
        List<ProteinSequence> sequences = chains.stream()
                .map(s -> {
                    try {
                        return new ProteinSequence(s);
                    } catch (CompoundNotFoundException e) {
                        log.warn("Unrecognized nucleotide in chain");
                        return null;
                    }
                })
                .collect(Collectors.toList());
        List<PairwiseSequenceScorer<ProteinSequence, AminoAcidCompound>> scorers = Alignments.getAllPairsScorers(sequences, Alignments.PairwiseSequenceScorerType.GLOBAL_IDENTITIES, gapPenalty, mat);
        Alignments.runPairwiseScorers(scorers);
        GuideTree<ProteinSequence, AminoAcidCompound> tree = new GuideTree<>(sequences, scorers);
        SimpleProfile<ProteinSequence, AminoAcidCompound> profile = (SimpleProfile<ProteinSequence, AminoAcidCompound>) Alignments.getProgressiveAlignment(tree, Alignments.ProfileProfileAlignerType.GLOBAL, gapPenalty, mat);
        return profile;
    }

    private SimpleProfile<DNASequence, NucleotideCompound> computeProgressiveDNAAlignment(List<String> chains, GapPenalty gapPenalty, SubstitutionMatrix subMatrix) {
        List<DNASequence> sequences = chains.stream()
                .map(s -> {
                    try {
                        return new DNASequence(s, AmbiguityDNACompoundSet.getDNACompoundSet());
                    } catch (CompoundNotFoundException e) {
                        log.warn("Unrecognized nucleotide in chain");
                        return null;
                    }
                })
                .collect(Collectors.toList());
        List<PairwiseSequenceScorer<DNASequence, NucleotideCompound>> scorers = Alignments.getAllPairsScorers(sequences, Alignments.PairwiseSequenceScorerType.GLOBAL_IDENTITIES, gapPenalty, subMatrix);
        Alignments.runPairwiseScorers(scorers);
        GuideTree<DNASequence, NucleotideCompound> tree = new GuideTree<>(sequences, scorers);
        SimpleProfile<DNASequence, NucleotideCompound> profile = (SimpleProfile<DNASequence, NucleotideCompound>) Alignments.getProgressiveAlignment(tree, Alignments.ProfileProfileAlignerType.GLOBAL, gapPenalty, subMatrix);
        return profile;
    }

    private SimpleProfile<DNASequence, NucleotideCompound> computeDNAAlignment(List<String> chains, GapPenalty gapPenalty, SubstitutionMatrix mat) {
        List<DNASequence> proteinSeqs = chains.stream()
                .map(s -> {
                    try {
                        return new DNASequence(s, AmbiguityDNACompoundSet.getDNACompoundSet());
                    } catch (CompoundNotFoundException e) {
                        log.warn("Unrecognized nucleotide in chain");
                        return null;
                    }
                })
                .collect(Collectors.toList());

        SimpleProfile<DNASequence, NucleotideCompound> profile = (SimpleProfile<DNASequence, NucleotideCompound>) Alignments.getMultipleSequenceAlignment(proteinSeqs, gapPenalty, mat);
        return profile;
    }

    private SimpleProfile<ProteinSequence, AminoAcidCompound> computeProteinAlignment(List<String> chains, GapPenalty gapPenalty, SubstitutionMatrix mat) {
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

        SimpleProfile<ProteinSequence, AminoAcidCompound> profile = (SimpleProfile<ProteinSequence, AminoAcidCompound>) Alignments.getMultipleSequenceAlignment(proteinSeqs, gapPenalty, mat);
        return profile;
    }

    private void fillProteinProfileMatrix(StringProperty matrixLabel, SimpleProfile<ProteinSequence, AminoAcidCompound> profile) {
        List<String> result = new ArrayList<>(profile.getSize() + 1);
        result.add("\t");
        profile.getCompoundSet().getAllCompounds().stream().forEach(c -> result.add(c.getShortName() + "\t"));

        IntStream.range(1, profile.getLength() + 1).forEach(i -> {
            int[] countsAt = profile.getCompoundCountsAt(i);
            long count = profile.getCompoundsAt(i).stream().filter(c -> c.equals(profile.getCompoundSet().getCompoundForString(GAP))).count();
            countsAt[profile.getCompoundSet().getAllCompounds().indexOf(profile.getCompoundSet().getCompoundForString(GAP))] = (int) count;
            List<Double> doubles = IntStream.range(0, countsAt.length).mapToDouble(idx -> (double) countsAt[idx] / profile.getSize()).boxed().collect(Collectors.toList());

            result.set(0, String.format("%s %-6s", result.get(0), i));
            for (int j = 0; j < doubles.size(); j++) {
                result.set(j + 1, String.format("%s %-6s", result.get(j + 1), String.format("%.2f", doubles.get(j))));
            }
        });
        matrixLabel.setValue(StringUtils.join(result, "\n"));
    }

    private void fillDNAProfileMatrix(StringProperty matrixLabel, Profile<DNASequence, NucleotideCompound> profile) {
        List<String> result = new ArrayList<>(profile.getSize() + 1);
        result.add("\t");
        profile.getCompoundSet().getAllCompounds().stream().forEach(c -> result.add(c.getShortName() + "\t"));

        IntStream.range(1, profile.getLength() + 1).forEach(i -> {
            int[] countsAt = profile.getCompoundCountsAt(i);
            long count = profile.getCompoundsAt(i).stream().filter(c -> c.equals(profile.getCompoundSet().getCompoundForString(GAP))).count();
            countsAt[profile.getCompoundSet().getAllCompounds().indexOf(profile.getCompoundSet().getCompoundForString(GAP))] = (int) count;
            List<Double> doubles = IntStream.range(0, countsAt.length).mapToDouble(idx -> (double) countsAt[idx] / profile.getSize()).boxed().collect(Collectors.toList());

            result.set(0, String.format("%s %-6s", result.get(0), i));
            for (int j = 0; j < doubles.size(); j++) {
                result.set(j + 1, String.format("%s %-6s", result.get(j + 1), String.format("%.2f", doubles.get(j))));
            }
        });
        matrixLabel.setValue(StringUtils.join(result, "\n"));
    }

    private void fillBox(String seq, HBox box) {
        Color color = Color.WHITE;
        Label label = new Label(seq);
        label.setFont(font("Monospace"));
        label.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
        box.getChildren().add(label);
    }

    private void fillResult(List<String> inputs, List<String> outputs, VBox inputsBox, VBox outputsBox) {
        if (inputsBox != null) {
            inputsBox.getChildren().clear();
        }
        outputsBox.getChildren().clear();

        if (inputs.size() != outputs.size()) {
            log.warn("The number of inputs doesn't equal the number of outputs");
            return;
        }
        final int rows = inputs.size();

        for (int i = 0; i < rows; ++i) {
            if (inputsBox != null) {
                HBox box1 = new HBox();
                fillBox(inputs.get(i), box1);
                inputsBox.getChildren().add(box1);
            }

            HBox box2 = new HBox();
            fillBox(outputs.get(i), box2);
            outputsBox.getChildren().add(box2);
        }
    }

}
