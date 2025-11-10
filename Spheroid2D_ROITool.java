import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * 2D Spheroid ROI Measurement Tool
 * Porting da macro ImageJ a plugin Java
 * Autore: Simonetta Croci (Università di Parma)
 * Compatibile con Fiji 1.54p / Java 8
 */
public class Spheroid2D_ROITool implements PlugIn {

    @Override
    public void run(String arg) {

        // === FOLDER SELECTION ===
        IJ.showMessage("Select Data Folder",
            "Please select the folder that contains your .tif images to process.\n\n" +
            "Each image will be analyzed and ROIs will be saved automatically.");

        String inputDir = IJ.getDirectory("→ Select the folder with .tif images");
        if (inputDir == null) return;

        IJ.showMessage("Select Output Folder",
            "Now select the folder where the plugin will save all result files:\n" +
            "- Individual ROI ZIP files\n" +
            "- Measurement CSV files\n" +
            "- Summary and log files");

        String outputDir = IJ.getDirectory("→ Select the folder to save results");
        if (outputDir == null) return;

        // === FILE PROCESSING AND VARIABLE PREPARATION ===
        String logPath = outputDir + "no_roi.txt";
        try (FileWriter logFile = new FileWriter(logPath)) {
            logFile.write("Images without ROI:\n");
        } catch (IOException e) {
            IJ.showMessage("Error", "Cannot create log file.");
        }

        File folder = new File(inputDir);
        String[] fileList = folder.list();
        if (fileList == null) {
            IJ.showMessage("Error", "No files found in folder.");
            return;
        }

        StringBuilder allResults = new StringBuilder();
        String resultsHeader = "";

        // ===  FILE LOOP ===
        for (String fileName : fileList) {
            if (!fileName.toLowerCase().endsWith(".tif")) continue;

            IJ.log("Processing: " + fileName);
            ImagePlus original = IJ.openImage(inputDir + fileName);
            if (original == null) continue;
            original.show();

            // Duplicazioni come nella macro
            IJ.run("Duplicate...", "title=Temp1");
            IJ.run("Duplicate...", "title=Temp1a");
            IJ.run("Duplicate...", "title=Temp2");
            IJ.run("Duplicate...", "title=Temp2a");
            IJ.run("Duplicate...", "title=Temp3");
            IJ.run("Duplicate...", "title=Temp3a");
            IJ.run("Duplicate...", "title=Temp4");

            RoiManager rm = RoiManager.getInstance();
            if (rm == null) rm = new RoiManager();
            rm.reset();

            // === FIRST ALTERNATIVE ===
            IJ.selectWindow("Temp1");
            IJ.run("8-bit");
            IJ.run("Unsharp Mask...", "radius=20 mask=0.70");
            IJ.run("Variance...", "radius=4");
            IJ.run("Median...", "radius=6");
            IJ.run("Make Binary");
            rm.reset();
            IJ.run("Analyze Particles...",
                "size=3000-Infinity circularity=0.20-0.90 display exclude summarize overlay add");
            IJ.selectWindow("Temp1a");
            rm.runCommand("Show All");
            new ij.gui.WaitForUserDialog("FIRST ALTERNATIVE", "Click OK to see the second.").show();

            // === SECOND ALTERNATIVE ===
            IJ.selectWindow("Temp2");
            IJ.run("8-bit");
            IJ.run("Unsharp Mask...", "radius=20 mask=0.70");
            IJ.run("Variance...", "radius=4");
            IJ.run("Median...", "radius=6");
            IJ.run("Make Binary");
            IJ.run("Invert");
            IJ.run("Fill Holes");
            rm.reset();
            IJ.run("Analyze Particles...",
                "size=3000-Infinity circularity=0.20-0.90 display exclude summarize overlay add");
            IJ.selectWindow("Temp2a");
            rm.runCommand("Show All");
            new ij.gui.WaitForUserDialog("SECOND ALTERNATIVE", "Click OK to see the third.").show();

            // === THIRD ALTERNATIVE ===
            IJ.selectWindow("Temp3");
            IJ.run("8-bit");
            IJ.run("Unsharp Mask...", "radius=30 mask=0.90");
            IJ.run("Auto Threshold", "method=Default");
            IJ.run("Convert to Mask");
            rm.reset();
            IJ.run("Analyze Particles...",
                "size=3000-Infinity circularity=0.20-0.90 display summarize overlay add");
            IJ.selectWindow("Temp3a");
            rm.runCommand("Show All");
            new ij.gui.WaitForUserDialog("THIRD ALTERNATIVE", "Click OK to choose.").show();

            // === USER CHOICE ===
            GenericDialog gd = new GenericDialog("Choose the best segmentation");
            gd.addChoice("Which alternative do you want to use?",
                new String[]{"First", "Second", "Third", "None - Skip"}, "First");
            gd.showDialog();
            if (gd.wasCanceled()) break;
            String altChoice = gd.getNextChoice();

            rm.reset();
            if (altChoice.equals("None - Skip")) {
                logImageWithoutROI(fileName, logPath);
                IJ.run("Close All");
                continue;
            }

            // === Image reprocessing based on the selected choice ===
            String sel = altChoice.equals("First") ? "Temp1a" :
                         altChoice.equals("Second") ? "Temp2a" : "Temp3a";
            IJ.selectWindow(sel);
            if (altChoice.equals("First")) {
                IJ.run("8-bit");
                IJ.run("Unsharp Mask...", "radius=20 mask=0.70");
                IJ.run("Variance...", "radius=4");
                IJ.run("Median...", "radius=6");
                IJ.run("Make Binary");
                IJ.run("Create Mask");
            } else if (altChoice.equals("Second")) {
                IJ.run("8-bit");
                IJ.run("Unsharp Mask...", "radius=20 mask=0.70");
                IJ.run("Variance...", "radius=4");
                IJ.run("Median...", "radius=6");
                IJ.run("Make Binary");
                IJ.run("Invert");
                IJ.run("Fill Holes");
            } else {
                IJ.run("8-bit");
                IJ.run("Unsharp Mask...", "radius=30 mask=0.90");
                IJ.run("Auto Threshold", "method=Default");
            }

            IJ.run("Analyze Particles...",
                "size=3000-Infinity circularity=0.20-0.90 display exclude summarize overlay add");
            IJ.run("Clear Results");
            IJ.selectWindow("Temp4");
            rm.runCommand("Show All");

            new ij.gui.WaitForUserDialog("Manual ROI Editing",
                "Delete unwanted ROIs and click OK when ready.").show();

            if (rm.getCount() == 0) {
                logImageWithoutROI(fileName, logPath);
                IJ.run("Close All");
                continue;
            }

            // === OPTIONAL ENLARGE ROIs ===
            GenericDialog gdEnlarge = new GenericDialog("Enlarge ROIs?");
            gdEnlarge.addChoice("Do you want to enlarge the ROIs?", new String[]{"No", "Yes"}, "No");
            gdEnlarge.showDialog();
            String enlargeChoice = gdEnlarge.getNextChoice();

    if (enlargeChoice.equals("Yes")) {
    boolean satisfied = false;
    while (!satisfied) {
        rm.setVisible(true);
        new ij.gui.WaitForUserDialog("ROI Selection",
            "Select the ROIs you want to enlarge in the ROI Manager, then click OK.").show();

        GenericDialog gdValue = new GenericDialog("ROI Enlarge Value");
        gdValue.addSlider("Enlarge value (pixels):", 0, 50, 10);
        gdValue.showDialog();
        int enlargeValue = (int) gdValue.getNextNumber();

        int[] selected = rm.getSelectedIndexes();
        if (selected.length == 0) {
            IJ.showMessage("No ROIs selected", "Please select at least one ROI in the ROI Manager.");
            continue;
        }

        // --- Applica l'enlarge a ogni ROI selezionata e aggiungi come nuova ROI ---
        for (int idx : selected) {
            rm.select(idx);
            IJ.run("Enlarge...", "enlarge=" + enlargeValue);
            // salva la ROI appena allargata come nuova
            ij.gui.Roi newRoi = IJ.getImage().getRoi();
            if (newRoi != null) {
                newRoi.setName(rm.getRoi(idx).getName() + "_enlarged");
                rm.addRoi(newRoi);
            }
        }

        // --- Mostra tutte le ROI nel RoiManager ---
        rm.runCommand("Show All");
        IJ.getImage().updateAndDraw();

        GenericDialog confirm = new GenericDialog("Preview Applied");
        confirm.addMessage(
            "The enlarged ROIs have been added to the ROI Manager.\n" +
            "You can now review them visually (originals + enlarged).\n\n" +
            "If you wish, delete unwanted ROIs manually in the ROI Manager.\n\n" +
            "When ready, click 'OK - Proceed' to continue, or 'Continue modifying' to try again."
        );
        confirm.addChoice("Next action:",
                new String[]{"Continue modifying", "OK - Proceed"}, "OK - Proceed");
        confirm.showDialog();
        String nextAction = confirm.getNextChoice();

        if (nextAction.equals("OK - Proceed")) {
            satisfied = true;
        }
    }

    new ij.gui.WaitForUserDialog("Final ROI Review",
        "Please delete any unwanted ROIs in the ROI Manager, then click OK to continue.").show();
}

            // === MEASUREMENT AND EXPORT ===
            IJ.selectWindow("Temp4");
            for (int i = 0; i < rm.getCount(); i++) {
                rm.select(i);
                rm.runCommand("Measure");
            }

            String baseName = fileName.contains(".") ?
                    fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
            rm.runCommand("Save", outputDir + baseName + "_ROIs.zip");
            IJ.saveAs("Results", outputDir + baseName + "_Results.csv");

            // === CREAZIONE RIASSUNTO ===
            try (Scanner sc = new Scanner(new File(outputDir + baseName + "_Results.csv"))) {
                String headerLine = sc.hasNextLine() ? sc.nextLine() : "";
                if (resultsHeader.equals("")) resultsHeader = "Name," + headerLine;
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if (!line.trim().isEmpty())
                        allResults.append(baseName).append(",").append(line).append("\n");
                }
            } catch (IOException e) {
                IJ.log("Error reading results: " + fileName);
            }

            rm.reset();
            IJ.run("Clear Results");
            IJ.run("Close All");
        }

        // === SALVA FILE RIASSUNTIVO ===
        if (!resultsHeader.isEmpty()) {
            try (FileWriter summary = new FileWriter(outputDir + "All_Results.csv")) {
                summary.write(resultsHeader + "\n");
                summary.write(allResults.toString());
                IJ.log("Summary saved: " + outputDir + "All_Results.csv");
            } catch (IOException e) {
                IJ.showMessage("Error saving summary file.");
            }
        } else {
            IJ.log("No results to save.");
        }

        IJ.showMessage("Done", "All images processed successfully.");
        IJ.run("Close All");
    }

    private void logImageWithoutROI(String name, String logPath) {
        try (FileWriter logFile = new FileWriter(logPath, true)) {
            logFile.write(name + "\n");
        } catch (IOException e) {
            IJ.log("Cannot write to log for: " + name);
        }
    }
}
