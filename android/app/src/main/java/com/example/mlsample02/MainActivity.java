package com.example.mlsample02;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ViewGroup cloudTextRecResultView;
    ViewGroup bundleTextRecResultTextView;
    ViewGroup bundleBarcodeRecResultTextView;
    ViewGroup bundleImageLabelResultTextView;
    ImageView captureImage;
    TextView translatedTextView;
    String targetTranslateText = "This is a pen.";
    boolean hasDownlodTranslataMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // output recognized text field
        cloudTextRecResultView = (ViewGroup) findViewById(R.id.cloudTextRecognize);
        bundleTextRecResultTextView = (ViewGroup) findViewById(R.id.bunldTextRecognize);
        bundleBarcodeRecResultTextView = (ViewGroup) findViewById(R.id.bundleBarcodeRecognize);
        bundleImageLabelResultTextView = (ViewGroup) findViewById(R.id.bundleImageLabel);
        translatedTextView = findViewById(R.id.translatedText);

        // capture image field
        captureImage = findViewById(R.id.imageView);

        Button bt_sl = findViewById(R.id.button);
        bt_sl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 初期化
                cloudTextRecResultView.removeAllViews();
                bundleTextRecResultTextView.removeAllViews();
                bundleBarcodeRecResultTextView.removeAllViews();
                bundleImageLabelResultTextView.removeAllViews();
                captureImage.setImageBitmap(null);
                translatedTextView.setText(null);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                ContentResolver contentResolver = getContentResolver();
                InputStream inputStream = null;
                Bitmap bitmap = null;
                try {
                    // get image data from Intent.getData
                    inputStream = contentResolver.openInputStream(data.getData());

                    // convert to bitmap
                    bitmap = BitmapFactory.decodeStream(inputStream);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (bitmap == null) {
                    new AlertDialog.Builder(this)
                            .setTitle("error")
                            .setMessage("error get image")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                captureImage.setImageBitmap(bitmap);

                /**
                 * Use Cloud Api
                 * */
                FirebaseVisionImage visionImage = FirebaseVisionImage.fromBitmap(bitmap);
                // Recognize Text   (Support Japanese)
                FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance().getCloudDocumentTextRecognizer();
                Task<FirebaseVisionDocumentText> result = detector.processImage(visionImage)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText result) {
                                // Task completed successfully
                                List<FirebaseVisionDocumentText.Block> blocks = result.getBlocks();
                                getLayoutInflater().inflate(R.layout.recognized_text_result, cloudTextRecResultView);
                                TableRow header = (TableRow) cloudTextRecResultView.getChildAt(0);
                                ((TextView) (header.getChildAt(0))).setText("recognized result");
                                ((TextView) (header.getChildAt(1))).setText("top left");
                                ((TextView) (header.getChildAt(2))).setText("top right");
                                ((TextView) (header.getChildAt(3))).setText("bottom right");
                                ((TextView) (header.getChildAt(4))).setText("bottom left");
                                header.setBackgroundColor(Color.LTGRAY);
                                for (int i = 0; i < blocks.size(); i++) {
                                    // per Block
                                    FirebaseVisionDocumentText.Block block = blocks.get(i);
                                    Rect rect = block.getBoundingBox(); //Text Rect
                                    getLayoutInflater().inflate(R.layout.recognized_text_result, cloudTextRecResultView);
                                    TableRow tr = (TableRow) cloudTextRecResultView.getChildAt(i + 1); //recognized text
                                    ((TextView) (tr.getChildAt(0))).setText(block.getText());
                                    ((TextView) (tr.getChildAt(1))).setText(String.format("(%d, %d)", rect.left, rect.top)); //corner top left
                                    ((TextView) (tr.getChildAt(2))).setText(String.format("(%d, %d)", rect.right, rect.top)); //corner top right
                                    ((TextView) (tr.getChildAt(3))).setText(String.format("(%d, %d)", rect.right, rect.bottom)); //corner bottom right
                                    ((TextView) (tr.getChildAt(4))).setText(String.format("(%d, %d)", rect.left, rect.bottom)); //corner bottom left
                                }

                                try {
                                    detector.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                try {
                                    detector.close();
                                } catch (Exception ex) {
                                    e.printStackTrace();
                                }
                            }
                        });

                /**
                 * Use Bundle library
                 * */
                InputImage bundleImage = InputImage.fromBitmap(bitmap, 0);
                // Recognize text https://developers.google.com/ml-kit/vision/text-recognition/android
                // Non-support Japanese,Chinese,Korean. You want recognize that, you need to use CloudApi.
                TextRecognizer recognizer = TextRecognition.getClient();
                Task<Text> textRecognizedResult =
                        recognizer.process(bundleImage)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text visionText) {
                                        // Task completed successfully
                                        List<Text.TextBlock> blocks = visionText.getTextBlocks();
                                        getLayoutInflater().inflate(R.layout.recognized_text_result, bundleTextRecResultTextView);
                                        TableRow header = (TableRow) bundleTextRecResultTextView.getChildAt(0);
                                        ((TextView) (header.getChildAt(0))).setText("recognized result");
                                        ((TextView) (header.getChildAt(1))).setText("top left");
                                        ((TextView) (header.getChildAt(2))).setText("top right");
                                        ((TextView) (header.getChildAt(3))).setText("bottom right");
                                        ((TextView) (header.getChildAt(4))).setText("bottom left");
                                        header.setBackgroundColor(Color.LTGRAY);
                                        for (int i = 0; i < blocks.size(); i++) {
                                            // per Block
                                            List<Text.Line> lines = blocks.get(i).getLines();
                                            Point[] point = blocks.get(i).getCornerPoints();
                                            if (point != null) {
                                                getLayoutInflater().inflate(R.layout.recognized_text_result, bundleTextRecResultTextView);
                                                TableRow tr = (TableRow) bundleTextRecResultTextView.getChildAt(i + 1); //recognized text
                                                ((TextView) (tr.getChildAt(0))).setText(blocks.get(i).getText());
                                                // https://developers.google.com/android/reference/com/google/mlkit/vision/text/Text.TextBlock#getCornerPoints()
                                                ((TextView) (tr.getChildAt(1))).setText(String.format("(%d, %d)", point[0].x, point[0].y)); //corner top left
                                                ((TextView) (tr.getChildAt(2))).setText(String.format("(%d, %d)", point[1].x, point[1].y)); //corner top right
                                                ((TextView) (tr.getChildAt(3))).setText(String.format("(%d, %d)", point[2].x, point[2].y)); //corner bottom right
                                                ((TextView) (tr.getChildAt(4))).setText(String.format("(%d, %d)", point[3].x, point[3].y)); //corner bottom left
                                            }
                                        }

                                        recognizer.close();
                                    }
                                })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Task failed with an exception
                                                recognizer.close();
                                                e.printStackTrace();
                                            }
                                        });

                // Scan barcode https://developers.google.com/ml-kit/vision/barcode-scanning/android#java
                // set paramater
                BarcodeScannerOptions options =
                        new BarcodeScannerOptions.Builder()
                                /**
                                 .setBarcodeFormats(
                                 Barcode.FORMAT_QR_CODE, //QR Code
                                 Barcode.FORMAT_CODABAR, //Codabar
                                 Barcode.FORMAT_CODE_128,
                                 Barcode.FORMAT_CODE_93,
                                 Barcode.FORMAT_CODE_39,
                                 Barcode.FORMAT_EAN_8, // JAN Code
                                 Barcode.FORMAT_EAN_13) // JAN Code
                                 **/
                                .build();
                BarcodeScanner scanner = BarcodeScanning.getClient(options);

                Task<List<Barcode>> barcodeScanResult = scanner.process(bundleImage)
                        .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                            @Override
                            public void onSuccess(List<Barcode> barcodes) {
                                // Task completed successfully
                                getLayoutInflater().inflate(R.layout.recognized_barcode_result, bundleBarcodeRecResultTextView);
                                TableRow header = (TableRow) bundleBarcodeRecResultTextView.getChildAt(0);
                                ((TextView) (header.getChildAt(0))).setText("recognized result");
                                ((TextView) (header.getChildAt(1))).setText("barcode type");
                                ((TextView) (header.getChildAt(2))).setText("top left");
                                ((TextView) (header.getChildAt(3))).setText("top right");
                                ((TextView) (header.getChildAt(4))).setText("bottom right");
                                ((TextView) (header.getChildAt(5))).setText("bottom left");
                                header.setBackgroundColor(Color.LTGRAY);
                                for (int i = 0; i < barcodes.size(); i++) {
                                    // per Block
                                    Point[] point = barcodes.get(i).getCornerPoints();
                                    if (point != null) {
                                        getLayoutInflater().inflate(R.layout.recognized_barcode_result, bundleBarcodeRecResultTextView);
                                        TableRow tr = (TableRow) bundleBarcodeRecResultTextView.getChildAt(i + 1); //recognized text
                                        ((TextView) (tr.getChildAt(0))).setText(barcodes.get(i).getDisplayValue());
                                        ((TextView) (tr.getChildAt(1))).setText(BarcodeType.getById(barcodes.get(i).getFormat()).getLabel());
                                        // https://developers.google.com/android/reference/com/google/mlkit/vision/barcode/Barcode#getCornerPoints()
                                        ((TextView) (tr.getChildAt(2))).setText(String.format("(%d, %d)", point[0].x, point[0].y)); //corner top left
                                        ((TextView) (tr.getChildAt(3))).setText(String.format("(%d, %d)", point[1].x, point[1].y)); //corner top right
                                        ((TextView) (tr.getChildAt(4))).setText(String.format("(%d, %d)", point[2].x, point[2].y)); //corner bottom right
                                        ((TextView) (tr.getChildAt(5))).setText(String.format("(%d, %d)", point[3].x, point[3].y)); //corner bottom left
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });

                // Image labeling
                ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
                labeler.process(bundleImage)
                        .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                            @Override
                            public void onSuccess(List<ImageLabel> labels) {
                                // Task completed successfully
                                getLayoutInflater().inflate(R.layout.label_image_result, bundleImageLabelResultTextView);
                                TableRow header = (TableRow) bundleImageLabelResultTextView.getChildAt(0);
                                ((TextView) (header.getChildAt(0))).setText("Label");
                                ((TextView) (header.getChildAt(1))).setText("Confidence");
                                header.setBackgroundColor(Color.LTGRAY);
                                for (int i = 0; i < labels.size(); i++) {
                                    getLayoutInflater().inflate(R.layout.label_image_result, bundleImageLabelResultTextView);
                                    TableRow tr = (TableRow) bundleImageLabelResultTextView.getChildAt(i + 1);
                                    // https://developers.google.com/ml-kit/vision/image-labeling/android
                                    ((TextView) (tr.getChildAt(0))).setText(labels.get(i).getText());
                                    ((TextView) (tr.getChildAt(1))).setText(String.format("%f", labels.get(i).getConfidence())); //corner top left
                                }

                                labeler.close();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                labeler.close();
                                e.printStackTrace();
                            }
                        });

                // Create an English-JAPANESE translator:
                TranslatorOptions translationOptions =
                        new TranslatorOptions.Builder()
                                .setSourceLanguage(TranslateLanguage.ENGLISH)
                                .setTargetLanguage(TranslateLanguage.JAPANESE)
                                .build();
                final Translator englishJapaneseTranslator = Translation.getClient(translationOptions);

                if (!hasDownlodTranslataMode) {
                    DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
                    englishJapaneseTranslator.downloadModelIfNeeded(conditions)
                            .addOnSuccessListener(
                                    new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            // Model downloaded successfully. Okay to start translating.
                                            hasDownlodTranslataMode = true;
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Model couldn’t be downloaded or other internal error.
                                            // ...
                                        }
                                    });
                }

                if (hasDownlodTranslataMode) {
                    englishJapaneseTranslator.translate(targetTranslateText)
                            .addOnSuccessListener(
                                    new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(@NonNull Object result) {
                                            // Translation successful.
                                            String translatedText = (String) result;
                                            translatedTextView.setText("(Translated Result)" + translatedText);
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Error.
                                            // ...
                                        }
                                    });
                }
            }
        }
    }

    // https://developers.google.com/android/reference/com/google/mlkit/vision/barcode/Barcode.BarcodeFormat
    public enum BarcodeType {
        FORMAT_UNKNOWN("FORMAT_UNKNOWN", -1),
        FORMAT_ALL_FORMATS("FORMAT_ALL_FORMATS", 0),
        FORMAT_CODE_128("FORMAT_CODE_128", 1),
        FORMAT_CODE_39("FORMAT_CODE_39", 2),
        FORMAT_CODE_93("FORMAT_CODE_93", 4),
        FORMAT_CODABAR("FORMAT_CODABAR", 8),
        FORMAT_DATA_MATRIX("FORMAT_DATA_MATRIX", 16),
        FORMAT_EAN_13("FORMAT_EAN_13", 32),
        FORMAT_EAN_8("FORMAT_EAN_8", 64),
        FORMAT_ITF("FORMAT_ITF", 128),
        FORMAT_QR_CODE("FORMAT_QR_CODE", 256),
        FORMAT_UPC_A("FORMAT_UPC_A", 512),
        FORMAT_UPC_E("FORMAT_UPC_E", 1024),
        FORMAT_PDF417("FORMAT_PDF417", 2048),
        FORMAT_AZTEC("FORMAT_AZTEC", 4096),
        TYPE_UNKNOWN("TYPE_UNKNOWN", 0),
        TYPE_CONTACT_INFO("TYPE_CONTACT_INFO", 1),
        TYPE_EMAIL("TYPE_EMAIL", 2),
        TYPE_ISBN("TYPE_ISBN", 3),
        TYPE_PHONE("TYPE_PHONE", 4),
        TYPE_PRODUCT("TYPE_PRODUCT", 5),
        TYPE_SMS("TYPE_SMS", 6),
        TYPE_TEXT("TYPE_TEXT", 7),
        TYPE_URL("TYPE_URL", 8),
        TYPE_WIFI("TYPE_WIFI", 9),
        TYPE_GEO("TYPE_GEO", 10),
        TYPE_CALENDAR_EVENT("TYPE_CALENDAR_EVENT", 11),
        TYPE_DRIVER_LICENSE("TYPE_DRIVER_LICENSE", 12);

        private String label;
        private int id;

        private BarcodeType(String label, int id) {
            this.label = label;
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public int getId() {
            return id;
        }

        public static BarcodeType getById(int id) {
            for (BarcodeType barcodeType : BarcodeType.values()) {
                if (barcodeType.getId() == id) {
                    return barcodeType;
                }
            }
            return null;
        }
    }
}