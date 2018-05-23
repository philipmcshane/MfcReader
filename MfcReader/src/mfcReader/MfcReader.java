/**
 * 
 */
package mfcReader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * @author Philip McShane
 * This program can be used to read the .mfc feature files produced by the CMU Sphinx speech recognition  toolkit
 *
 */
public class MfcReader extends Application implements Initializable {

	@FXML
	private Button btn;

	@FXML
	private TextArea tArea;

	@FXML
	private TextField len;

	private String fileName;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		fileName = "";
		Parent root = FXMLLoader.load(getClass().getResource("View.fxml"));

		Scene scene = new Scene(root, 900, 600);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	@FXML
	private void onOpen() {
		// Reference to stage to which button belongs
		Stage stage = (Stage) btn.getScene().getWindow();
		// Create file chooser
		FileChooser fc = new FileChooser();

		// Show file chooser
		File file = fc.showOpenDialog(stage);
		if (file != null) {
			processMFC(file);
		}
	}

	@FXML
	private void clear() {
		tArea.clear();
		fileName = "";
	}

	private void processMFC(File file) {
		int header = 0;
		int fileByteSize = 0;
		int rowPos = 0;
		float coefficent = 0.0f;
		int featureLength = 13;
		StringBuilder sb = new StringBuilder();
		String formatFloat = "";
		boolean littleEndian = false;

		String str = len.getText();

		if (str.length() > 0) {
			featureLength = Integer.valueOf(str);
		}

		sb.append(file.getName() + "\n");

		try (InputStream in = new FileInputStream(file);
				DataInputStream dis = new DataInputStream(in)) {
			header = dis.readInt();
			fileByteSize = header * 4 + 4;
			if (fileByteSize != file.length()) {
				littleEndian = true;
				// Litte endian so reverse headers bytes to get correct value
				header = swapInt(header);
				fileByteSize = header * 4 + 4;
			}

			sb.append(header + "\n");
			if (!littleEndian) {
				for (int count = 0; count < header; count++) {
					coefficent = dis.readFloat();
					formatFloat = String.format("%.3f  ", coefficent);
					sb.append(formatFloat);

					// If row as same number of values as feature length take
					// new
					// line
					if (rowPos == featureLength) {
						sb.append("\n");
						rowPos = 0;
					}
					rowPos++;
				}

			} else {
				// Allocate buffer to hold
				ByteBuffer byteBuff = ByteBuffer
						.allocate((int) (fileByteSize - 4));
				for (int count = 0; count < header; count++) {
					byteBuff.putFloat(dis.readFloat());
				}

				// Set byte order
				byteBuff.order(ByteOrder.LITTLE_ENDIAN);

				// Ensure buffer will be read from the start
				byteBuff.position(0);

				// Now bytes in right order add to string builder
				for (int count = 0; count < header; count++, rowPos++) {
					coefficent = byteBuff.getFloat();
					if (coefficent > 0.0) {
						formatFloat = String
								.format("%2s%7.3f%s ", "", coefficent, "");
					} else {
						formatFloat = String
								.format("%1s%7.3f%s ", "", coefficent,"");
					}
					sb.append(formatFloat);

					// Take a new line when end of frame reached
					if (rowPos == featureLength-1) {
						sb.append("\n");
						rowPos = -1;
					}
				}
			}

			// Add lines for formatting
			sb.append("\n\n");

			// Append text to text area
			tArea.appendText(sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		tArea.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();

				if (db.hasFiles()) {

					for (File f : db.getFiles()) {
						// Only read file if file has not already been read
						if (!f.getName().equalsIgnoreCase(fileName)) {
							processMFC(f);
						}
						fileName = f.getName();
					}
				}
			}

		});

		// tArea.setWrapText(true);
	}

	/**
	 * Reverse byte order of int. Can change big endian to little endian and
	 * vice-versa.
	 * 
	 * @param i
	 *            - int to reverse
	 * @return - int with reverse endianess
	 */
	public int swapInt(int i) {
		return ((i << 24) + ((i << 8) & 0x00FF0000)) + ((i >> 8) & 0x0000FF00)
				+ (i >>> 24);
	}
	
	@FXML
	private void help() {
		tArea.appendText("This Program can be used to read mfc files.\n To Open a file either press open and select the file or drag the file over the text area.\nThe default feature length is 13 to change this simply enter a different value in the feature length text field.\nTo remove all text click the \"Clear\" button");

	}
}
