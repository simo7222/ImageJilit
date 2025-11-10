# 2D Spheroid ROI Measurement Tool

**Author:** Simonetta Croci (University of Parma)  
**Compatibility:** Fiji 1.54p / Java 8  

This ImageJ/Fiji plugin is a Java port of a macro for measuring Regions of Interest (ROIs) in 2D spheroid images. The tool allows automatic processing of multiple `.tif` images, applying different segmentation alternatives, manually selecting the best one, and saving results in CSV and ZIP files.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Workflow](#workflow)
- [Output](#output)
- [Error Logging](#error-logging)
- [License](#license)

---

## Features

- Batch processing of `.tif` images
- Automatic segmentation with three alternatives
- Manual selection of the best segmentation
- Option to enlarge selected ROIs
- Save individual results and a summary file
- Error log for images without ROIs

---

## Requirements

- Fiji 1.54p
- Java 8
- Standard ImageJ libraries (`ij.jar` included in Fiji)

---

## Installation

1. Copy the `Spheroid2D_ROITool.java` file into Fiji's plugins folder:


2. Compile the plugin via the `Plugins > Compile and Run` menu in Fiji.
3. Restart Fiji if necessary.

---

## Usage

1. Launch the plugin from the `Plugins` menu.
2. Select the folder containing `.tif` images.
3. Select the output folder where results, ROIs, and logs will be saved.
4. Follow the dialog windows to:
- Preview segmentation alternatives
- Choose the best segmentation
- Manually edit or enlarge ROIs if needed
5. Save results. The plugin generates:
- Individual CSV files per image
- ZIP files containing ROIs
- Summary file `All_Results.csv`
- Log `no_roi.txt` for images without ROIs

---

## Workflow

1. Duplicate the original image to create temporary images for each segmentation alternative.
2. Apply three segmentation alternatives:
- **First alternative:** filters, binarization, particle analysis
- **Second alternative:** similar to first but with inversion and hole filling
- **Third alternative:** filters, automatic thresholding, mask conversion
3. User selects the best alternative through a dialog.
4. Optional enlargement of selected ROIs.
5. Measure and save results.
6. Create a global summary file with all processed images.

---

## Output

For each processed image:

- `ImageName_ROIs.zip` → Saved ROIs
- `ImageName_Results.csv` → Measurements
- `All_Results.csv` → Summary of all images
- `no_roi.txt` → Images without detected ROIs

---

## Error Logging

- If an image contains no ROIs, its name is added to `no_roi.txt` in the output folder.
- File read/write errors are logged in the ImageJ/Fiji console.

---

## License

This plugin is released for **academic and research use**. Modifications and redistribution are allowed with proper attribution to the original author.
