import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/*
 * ImageJ Plugin to open 4 channel deltavision image containing
 * DAPI, GFP, RFP and Far Red labels. User identifies the channels
 * and an area of background for subtraction in each colour channel.
 * The user selects which cell to measure from the thresholded DAPI 
 * signal and this area is used to measure the intensity and areas of
 * each fluorescent dot within the ROI for each colour channel.   
 */

public class Alba_Region_Grow_Bkgrd_CENPC_3pixels implements PlugIn{
	String filename;
	ImagePlus BlueImage;
	int BlueImageID;
	ImagePlus GreenImage;
	int GreenImageID;
	ImagePlus RedImage;
	int RedImageID;
	ImagePlus FarRedImage;
	int FarRedImageID;
	int RoiIndexPos;
	double BkGrdGreen;
	double BkGrdRed;
    double BkGrdFarRed;
	double MeanInt[] = new double[60]; 
	double modeInt[] = new double[60]; 
	double minInt[]= new double[60];
	double maxInt[]= new double[60];
	double intDensity[]= new double[60];
	double medianInt[]= new double[60];
	double rawIntDensity[]= new double[60];
	String theparentDirectory;
	
	public void run(String arg) {
		
		/*
		 * Open the image and select the colour channels.
		 * Calls all the other functions in the plugin
		 */
		IJ.run("Set Measurements...", "area mean modal min centroid integrated median redirect=None decimal=2");
		new WaitForUserDialog("Open Image", "Open Images. SPLIT CHANNELS!").show();
		IJ.run("Bio-Formats Importer");
		
		ImagePlus imp = WindowManager.getCurrentImage();
		filename = imp.getTitle(); 	//Get file name
		FileInfo filedata = imp.getOriginalFileInfo();
		theparentDirectory = filedata.directory; //Get File Path
		
		
		new WaitForUserDialog("Select" , "Select Blue Image").show();
		BlueImage = WindowManager.getCurrentImage();
		BlueImageID = BlueImage.getID();
		IJ.run(BlueImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		
		new WaitForUserDialog("Select" , "Select Green Image").show();
		GreenImage = WindowManager.getCurrentImage();
		GreenImageID = GreenImage.getID();
		IJ.run(GreenImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		
		new WaitForUserDialog("Select" , "Select Red Image").show();
		RedImage = WindowManager.getCurrentImage();
		RedImageID = RedImage.getID();
		IJ.run(RedImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		
		new WaitForUserDialog("Select" , "Select Far Red Image").show();
		FarRedImage = WindowManager.getCurrentImage();
		FarRedImageID = FarRedImage.getID();
		IJ.run(FarRedImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
	
		selectBackround(); //Function to pick the area for background subtraction in GFP,RFP and Far Red
		
		RoiManager PickOne = new RoiManager(); 
		PickOne = SelectReferenceCells();	// Function for user to select cell of interest from ROI Manager
		CountChannels(PickOne); //Measure fluorescent dots in all the channles
		IJ.run("Close All", "");
		new WaitForUserDialog("Finished" , "The plugin has finished").show();
	}
	
	private void selectBackround(){
		
		/*
		 * Method asks user to draw an oval in an area of 
		 * background on each channel. This value is then used
		 * to subtract the background in each measurement
		 * channel
		 */
		IJ.setTool("oval");
		for(int b=0;b<3;b++){
			if (b==0){		
				IJ.selectWindow(GreenImageID);
				new WaitForUserDialog("BkGrd" , "Draw Background Oval").show();
				IJ.setThreshold(GreenImage, 0, 65535);
				IJ.run(GreenImage, "Measure", "");
				ResultsTable rt = new ResultsTable();	
				rt = Analyzer.getResultsTable();
				BkGrdGreen = rt.getValueAsDouble(1, 0);
				IJ.deleteRows(0, 0);
			}
			if (b==1){
				IJ.selectWindow(RedImageID);
				new WaitForUserDialog("BkGrd" , "Draw Background Oval").show();
				IJ.setThreshold(RedImage, 0, 65535);
				IJ.run(RedImage, "Measure", "");
				ResultsTable rt = new ResultsTable();	
				rt = Analyzer.getResultsTable();
				BkGrdRed = rt.getValueAsDouble(1, 0);
				IJ.deleteRows(0, 0);
			}
			if (b==2){
				IJ.selectWindow(FarRedImageID);
				new WaitForUserDialog("BkGrd" , "Draw Background Oval").show();
				IJ.setThreshold(FarRedImage, 0, 65535);
				IJ.run(FarRedImage, "Measure", "");
				ResultsTable rt = new ResultsTable();	
				rt = Analyzer.getResultsTable();
				BkGrdFarRed = rt.getValueAsDouble(1, 0);
				IJ.deleteRows(0, 0);
			}
		}

	}
	
	private RoiManager SelectReferenceCells(){
		
		/*
		 * Method autothresholds the nuclei in the blue
		 * channel. User is asked to make any fine 
		 * adjustments to make sure only the area of the 
		 * chromosomes is selected. The user picks the
		 * correct cell from the ROI Manager and this is 
		 * returned for use in the method to measure
		 * fluorescence in the other channels
		 */
		
		
		//Check ROI Manager is empty
		RoiManager TestEmpty = RoiManager.getInstance();
		int EmptyROI = TestEmpty.getCount();
		if (EmptyROI>0){
			TestEmpty.runCommand(BlueImage,"Deselect");
			TestEmpty.runCommand(BlueImage,"Delete");
		}
		//Select the correct cell
		IJ.selectWindow(BlueImageID);
		IJ.setAutoThreshold(BlueImage, "Default dark");
		IJ.run("Threshold...");
		new WaitForUserDialog("Adjust" , "Adjust nucleus threshold if required").show();
		IJ.run(BlueImage, "Analyze Particles...", "size=150-Infinity pixel exclude include add");
		RoiManager PickOne = RoiManager.getInstance();
		int RoiCount = PickOne.getCount();
		RoiIndexPos = 0;
		if(RoiCount>1){
			new WaitForUserDialog("Select" , "Select cell of interest from ROI Manager").show();
			RoiIndexPos = PickOne.getSelectedIndex();
		}
		
		return PickOne;
	
	}
	
	private void CountChannels(RoiManager PickOne){
		
		/*
		 * Method does all the counting in the fluorescent 
		 * channels based on the area provided SelectReferenceCells
		 * method
		 */
		IJ.selectWindow(RedImageID);
		
		IJ.run(RedImage, "Unsharp Mask...", "radius=1 mask=0.80");
		PickOne.select(RoiIndexPos);
		IJ.setAutoThreshold(RedImage, "Yen dark");
		IJ.run(RedImage, "Analyze Particles...", "size=0.02-3.00 show=Masks");
		ImagePlus MaskImage = WindowManager.getCurrentImage();
		int MaskImageID = MaskImage.getID();
		IJ.selectWindow(RedImageID);
		PickOne.runCommand(RedImage,"Deselect");
		PickOne.runCommand(RedImage,"Delete");
		IJ.selectWindow(MaskImageID);
		IJ.setAutoThreshold(MaskImage, "Default");
		IJ.run(MaskImage, "Analyze Particles...", "size=0.02-3.00 add");
		
		clearResults();
	
		RoiManager rm = RoiManager.getInstance();
		int NumRoi = rm.getCount();
		String roiDir = theparentDirectory.replace("\\", "\\\\");
		String roiFile = filename.replace(".", "");
		String roiName = roiDir + roiFile +".zip";
		rm.runCommand("Save", roiName);	
		
		IJ.selectWindow(RedImageID);
		for (int x=0;x<NumRoi;x++){
			rm.select(x);
			IJ.run(RedImage, "Cut", "");
			IJ.run(RedImage, "Enlarge...", "enlarge=3 pixel");
			IJ.setThreshold(RedImage, 3, 65535);
			IJ.run(RedImage, "Analyze Particles...", "size=0.02-3.00 display");
		}
		GetMeasurements(NumRoi);
		String colour = "Red";
		OutputText(colour,NumRoi);
		
		clearResults();
		
		IJ.selectWindow(GreenImageID);
		for (int x=0;x<NumRoi;x++){
			rm.select(x);
			IJ.run(GreenImage, "Cut", "");
			IJ.run(GreenImage, "Enlarge...", "enlarge=3 pixel");
			IJ.setThreshold(GreenImage, 3, 65535);
			IJ.run(GreenImage, "Analyze Particles...", "size=0.02-3.00 display");
		}
	
		GetMeasurements(NumRoi);
		colour = "Green";
		OutputText(colour,NumRoi);
		clearResults();
		
		IJ.selectWindow(FarRedImageID);
		for (int x=0;x<NumRoi;x++){
			rm.select(x);
			IJ.run(FarRedImage, "Cut", "");
			IJ.run(FarRedImage, "Enlarge...", "enlarge=3 pixel");
			IJ.setThreshold(FarRedImage, 3, 65535);
			IJ.run(FarRedImage, "Analyze Particles...", "size=0.02-3.00 display");
		}
		GetMeasurements(NumRoi);
		colour = "FarRed";
		OutputText(colour,NumRoi);
		
	}

	private void GetMeasurements(int NumRoi){
		/*
		 * Method gets all the measurements when called for each channel
		 * its in a separate method so that it only has to be written once
		 * rather than a copy for each colour channel.
		 */
		ResultsTable rt = new ResultsTable();	
		rt = Analyzer.getResultsTable();
		int valnums = rt.getCounter();
		
		for(int y=0;y<valnums;y++){
			MeanInt[y] = rt.getValueAsDouble(1, y);
			modeInt[y] = rt.getValueAsDouble(3, y);
			minInt[y] = rt.getValueAsDouble(4, y);
			maxInt[y] = rt.getValueAsDouble(5, y);
			intDensity[y] = rt.getValueAsDouble(20, y);
			medianInt[y] = rt.getValueAsDouble(21, y);
			rawIntDensity[y] = rt.getValueAsDouble(25, y);
		}
		
	}
	
	private void clearResults() {

		/*
		 * Clear the results table
		 */
		ResultsTable ClearRT = new ResultsTable();	
		ClearRT = Analyzer.getResultsTable();
		int numres = ClearRT.getCounter();
		if (numres > 0){
			IJ.deleteRows(0, numres);
		}
	}
	
	private void OutputText(String colour, int NumRoi){
		/*
		 * Method outputs alll the results into a text file 
		 * for import into Excel or R etc.
		 */
		String CreateName = "C:/Temp/Results.txt";
		String FILE_NAME = CreateName;
    	
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			if(colour.equals("Red")){
				bufferedWriter.newLine();
				bufferedWriter.newLine();
				bufferedWriter.write(" File= " + filename);
				bufferedWriter.newLine();
				bufferedWriter.write(" Green Background = " + BkGrdGreen);
				bufferedWriter.newLine();
				bufferedWriter.write(" Red Background = " + BkGrdRed);
				bufferedWriter.newLine();
				bufferedWriter.write(" FarRed Background = " + BkGrdFarRed);
				bufferedWriter.newLine();
				bufferedWriter.newLine();
			}
			
			for (int z = 0; z<NumRoi;z++){
				bufferedWriter.write(colour + " Dot = " + z + " Mean Intensity = " + MeanInt[z] + " Mode Intensity = " + modeInt[z] + " Min Intensity = " + minInt[z] + " Max Intensity = " + maxInt[z] + " Integrated Density = " + intDensity[z] + " Median Intensity = " + medianInt[z] + " Raw Integrated Density = " + rawIntDensity[z]);
				bufferedWriter.newLine();
			}
			
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	}
}
