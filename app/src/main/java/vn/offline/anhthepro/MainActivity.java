package vn.offline.anhthepro;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1001;

    private ImageView preview;
    private TextView status;

    private Bitmap original;
    private Bitmap person;
    private Bitmap result;

    private int bgColor = Color.WHITE;
    private float brightness = 1f;
    private float selectedRatio = 3f / 4f;

    private Segmenter segmenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SelfieSegmenterOptions options =
                new SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .enableRawSizeMask()
                        .build();

        segmenter = Segmentation.getClient(options);

        setContentView(buildUI());
    }

    private View buildUI() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 24, 28, 50);
        root.setBackgroundColor(Color.rgb(242, 247, 255));

        scroll.addView(root);

        TextView title = txt(
                "ẢNH THẺ PRO",
                29,
                true,
                Color.rgb(0, 72, 170)
        );

        root.addView(title);

        root.addView(txt(
                "Studio ảnh thẻ offline",
                15,
                false,
                Color.DKGRAY
        ));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams pp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        760
                );

        pp.setMargins(0, 20, 0, 15);

        root.addView(preview, pp);

        Button pick = btn("📷  CHỌN ẢNH CHÂN DUNG");

        pick.setOnClickListener(v -> {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            startActivityForResult(i, PICK_IMAGE);
        });

        root.addView(pick);

        status = txt(
                "Chọn ảnh để bắt đầu",
                14,
                false,
                Color.DKGRAY
        );

        root.addView(status);

        root.addView(section("TÁCH NỀN AI"));

        Button remove = btn("✨ TÁCH NỀN TỰ ĐỘNG");

        remove.setOnClickListener(v -> segmentPerson());

        root.addView(remove);

        root.addView(section("MÀU NỀN"));

        LinearLayout colors = row();

        colors.addView(action("Trắng", v -> setBackground(Color.WHITE)), weight());
        colors.addView(action("Xanh", v -> setBackground(Color.rgb(30, 125, 230))), weight());
        colors.addView(action("Xanh nhạt", v -> setBackground(Color.rgb(105, 185, 245))), weight());
        colors.addView(action("Đỏ", v -> setBackground(Color.rgb(215, 35, 45))), weight());

        root.addView(colors);

        root.addView(section("THAY ÁO OFFLINE"));

        LinearLayout clothes = row();

        clothes.addView(action(
                "Sơ mi",
                v -> applyClothes(0)
        ), weight());

        clothes.addView(action(
                "Vest đen",
                v -> applyClothes(1)
        ), weight());

        clothes.addView(action(
                "Vest xanh",
                v -> applyClothes(2)
        ), weight());

        root.addView(clothes);

        root.addView(section("KÍCH THƯỚC ẢNH"));

        LinearLayout sizes = row();

        sizes.addView(action(
                "2×3",
                v -> chooseRatio(2f / 3f, "2 × 3 cm")
        ), weight());

        sizes.addView(action(
                "3×4",
                v -> chooseRatio(3f / 4f, "3 × 4 cm")
        ), weight());

        sizes.addView(action(
                "4×6",
                v -> chooseRatio(2f / 3f, "4 × 6 cm")
        ), weight());

        root.addView(sizes);

        root.addView(section("ÁNH SÁNG"));

        SeekBar light = new SeekBar(this);
        light.setMax(200);
        light.setProgress(100);

        light.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        brightness = Math.max(0.35f, progress / 100f);
                        rebuild();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                }
        );

        root.addView(light);

        root.addView(section("HOÀN THÀNH"));

        Button crop = btn("✂ CĂN ẢNH THEO KÍCH THƯỚC");

        crop.setOnClickListener(v -> cropCurrent());

        root.addView(crop);

        Button save = btn("💾 LƯU ẢNH JPG");

        save.setOnClickListener(v -> saveImage());

        root.addView(save);

        TextView privacy = txt(
                "🔒 Ảnh được xử lý trực tiếp trên điện thoại.",
                13,
                false,
                Color.GRAY
        );

        LinearLayout.LayoutParams pv =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        pv.setMargins(0, 25, 0, 0);

        root.addView(privacy, pv);

        return scroll;
    }

    private void segmentPerson() {

        if (original == null) {
            toast("Anh chưa chọn ảnh.");
            return;
        }

        status.setText("Đang tách nền...");

        InputImage image =
                InputImage.fromBitmap(original, 0);

        segmenter.process(image)

                .addOnSuccessListener(mask -> {

                    person = createPersonBitmap(
                            original,
                            mask
                    );

                    result = composite(
                            person,
                            bgColor
                    );

                    preview.setImageBitmap(result);

                    status.setText(
                            "✓ Đã tách nền. Có thể chọn màu nền."
                    );
                })

                .addOnFailureListener(e -> {
                    status.setText("Không tách được nền.");
                    toast(e.getMessage());
                });
    }

    private Bitmap createPersonBitmap(
            Bitmap source,
            SegmentationMask mask
    ) {

        int mw = mask.getWidth();
        int mh = mask.getHeight();

        ByteBuffer buffer = mask.getBuffer();
        buffer.rewind();

        Bitmap alpha = Bitmap.createBitmap(
                mw,
                mh,
                Bitmap.Config.ARGB_8888
        );

        int[] pixels = new int[mw * mh];

        for (int i = 0; i < pixels.length; i++) {

            float confidence = buffer.getFloat();

            int a;

            if (confidence <= 0.25f) {
                a = 0;
            } else if (confidence >= 0.75f) {
                a = 255;
            } else {
                a = (int)(
                        255f *
                        ((confidence - 0.25f) / 0.5f)
                );
            }

            pixels[i] =
                    Color.argb(
                            a,
                            255,
                            255,
                            255
                    );
        }

        alpha.setPixels(
                pixels,
                0,
                mw,
                0,
                0,
                mw,
                mh
        );

        Bitmap scaledMask =
                Bitmap.createScaledBitmap(
                        alpha,
                        source.getWidth(),
                        source.getHeight(),
                        true
                );

        Bitmap out =
                Bitmap.createBitmap(
                        source.getWidth(),
                        source.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

        Canvas canvas = new Canvas(out);

        Paint p = new Paint(
                Paint.ANTI_ALIAS_FLAG |
                Paint.FILTER_BITMAP_FLAG
        );

        canvas.drawBitmap(source, 0, 0, p);

        p.setXfermode(
                new PorterDuffXfermode(
                        PorterDuff.Mode.DST_IN
                )
        );

        canvas.drawBitmap(
                scaledMask,
                0,
                0,
                p
        );

        p.setXfermode(null);

        return out;
    }

    private Bitmap composite(
            Bitmap transparent,
            int color
    ) {

        Bitmap out =
                Bitmap.createBitmap(
                        transparent.getWidth(),
                        transparent.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

        Canvas c = new Canvas(out);

        c.drawColor(color);

        Paint p = new Paint(
                Paint.ANTI_ALIAS_FLAG |
                Paint.FILTER_BITMAP_FLAG
        );

        ColorMatrix cm = new ColorMatrix();

        cm.setScale(
                brightness,
                brightness,
                brightness,
                1f
        );

        p.setColorFilter(
                new ColorMatrixColorFilter(cm)
        );

        c.drawBitmap(
                transparent,
                0,
                0,
                p
        );

        return out;
    }

    private void setBackground(int color) {

        bgColor = color;

        if (person != null) {
            rebuild();
        } else {
            toast("Tách nền trước anh nhé.");
        }
    }

    private void rebuild() {

        if (person == null) return;

        result = composite(
                person,
                bgColor
        );

        preview.setImageBitmap(result);
    }

    private void chooseRatio(
            float ratio,
            String name
    ) {

        selectedRatio = ratio;

        toast("Đã chọn " + name);
    }

    private void cropCurrent() {

        Bitmap src =
                result != null
                        ? result
                        : original;

        if (src == null) {
            toast("Anh chưa chọn ảnh.");
            return;
        }

        int w = src.getWidth();
        int h = src.getHeight();

        float current =
                (float) w / h;

        int newW = w;
        int newH = h;

        if (current > selectedRatio) {

            newW =
                    Math.round(
                            h * selectedRatio
                    );

        } else {

            newH =
                    Math.round(
                            w / selectedRatio
                    );
        }

        int x =
                Math.max(
                        0,
                        (w - newW) / 2
                );

        int y =
                Math.max(
                        0,
                        (h - newH) / 2
                );

        result =
                Bitmap.createBitmap(
                        src,
                        x,
                        y,
                        newW,
                        newH
                );

        preview.setImageBitmap(result);

        status.setText(
                "✓ Đã căn ảnh theo tỷ lệ đã chọn."
        );
    }

    private void applyClothes(int type) {

        Bitmap src =
                result != null
                        ? result
                        : original;

        if (src == null) {
            toast("Anh chưa chọn ảnh.");
            return;
        }

        Bitmap out =
                src.copy(
                        Bitmap.Config.ARGB_8888,
                        true
                );

        Canvas c = new Canvas(out);

        float w = out.getWidth();
        float h = out.getHeight();

        float top = h * 0.63f;

        Path body = new Path();

        body.moveTo(
                w * 0.17f,
                h
        );

        body.lineTo(
                w * 0.25f,
                top
        );

        body.quadTo(
                w * 0.35f,
                h * 0.58f,
                w * 0.43f,
                h * 0.62f
        );

        body.lineTo(
                w * 0.50f,
                h * 0.74f
        );

        body.lineTo(
                w * 0.57f,
                h * 0.62f
        );

        body.quadTo(
                w * 0.65f,
                h * 0.58f,
                w * 0.75f,
                top
        );

        body.lineTo(
                w * 0.83f,
                h
        );

        body.close();

        Paint cloth =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        if (type == 0) {
            cloth.setColor(
                    Color.rgb(
                            245,
                            245,
                            245
                    )
            );
        } else if (type == 1) {
            cloth.setColor(
                    Color.rgb(
                            28,
                            31,
                            38
                    )
            );
        } else {
            cloth.setColor(
                    Color.rgb(
                            22,
                            50,
                            88
                    )
            );
        }

        c.drawPath(body, cloth);

        Paint shirt =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        shirt.setColor(Color.WHITE);

        Path leftCollar =
                new Path();

        leftCollar.moveTo(
                w * 0.39f,
                h * 0.61f
        );

        leftCollar.lineTo(
                w * 0.50f,
                h * 0.74f
        );

        leftCollar.lineTo(
                w * 0.44f,
                h * 0.79f
        );

        leftCollar.close();

        c.drawPath(
                leftCollar,
                shirt
        );

        Path rightCollar =
                new Path();

        rightCollar.moveTo(
                w * 0.61f,
                h * 0.61f
        );

        rightCollar.lineTo(
                w * 0.50f,
                h * 0.74f
        );

        rightCollar.lineTo(
                w * 0.56f,
                h * 0.79f
        );

        rightCollar.close();

        c.drawPath(
                rightCollar,
                shirt
        );

        result = out;

        preview.setImageBitmap(result);

        status.setText(
                "✓ Đã ghép áo. Đây là mẫu áo offline."
        );
    }

    private void saveImage() {

        Bitmap src =
                result != null
                        ? result
                        : original;

        if (src == null) {
            toast("Chưa có ảnh để lưu.");
            return;
        }

        try {

            ContentValues values =
                    new ContentValues();

            values.put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "AnhThePro_" +
                    System.currentTimeMillis() +
                    ".jpg"
            );

            values.put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
            );

            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/AnhThePro"
            );

            Uri uri =
                    getContentResolver()
                            .insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    values
                            );

            if (uri == null) {
                toast("Không tạo được file.");
                return;
            }

            OutputStream os =
                    getContentResolver()
                            .openOutputStream(uri);

            src.compress(
                    Bitmap.CompressFormat.JPEG,
                    96,
                    os
            );

            os.close();

            status.setText(
                    "✓ Đã lưu vào Pictures/AnhThePro"
            );

            toast("Đã lưu ảnh.");
        }

        catch (Exception e) {
            toast(
                    "Lỗi lưu ảnh: " +
                    e.getMessage()
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode == PICK_IMAGE &&
                resultCode == RESULT_OK &&
                data != null
        ) {

            try {

                Uri uri =
                        data.getData();

                InputStream is =
                        getContentResolver()
                                .openInputStream(uri);

                Bitmap decoded =
                        BitmapFactory.decodeStream(is);

                if (is != null) {
                    is.close();
                }

                if (decoded == null) {
                    toast("Không đọc được ảnh.");
                    return;
                }

                int max = 1600;

                if (
                        decoded.getWidth() > max ||
                        decoded.getHeight() > max
                ) {

                    float scale =
                            Math.min(
                                    (float) max /
                                            decoded.getWidth(),

                                    (float) max /
                                            decoded.getHeight()
                            );

                    original =
                            Bitmap.createScaledBitmap(
                                    decoded,
                                    Math.round(
                                            decoded.getWidth() *
                                            scale
                                    ),
                                    Math.round(
                                            decoded.getHeight() *
                                            scale
                                    ),
                                    true
                            );

                } else {
                    original = decoded;
                }

                person = null;
                result = original;

                preview.setImageBitmap(
                        original
                );

                status.setText(
                        "✓ Đã chọn ảnh. Bấm TÁCH NỀN TỰ ĐỘNG."
                );
            }

            catch (Exception e) {

                toast(
                        "Không mở được ảnh: " +
                        e.getMessage()
                );
            }
        }
    }

    private LinearLayout row() {

        LinearLayout r =
                new LinearLayout(this);

        r.setOrientation(
                LinearLayout.HORIZONTAL
        );

        return r;
    }

    private LinearLayout.LayoutParams weight() {

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        p.setMargins(
                3,
                3,
                3,
                3
        );

        return p;
    }

    private Button action(
            String s,
            View.OnClickListener listener
    ) {

        Button b = btn(s);

        b.setOnClickListener(
                listener
        );

        return b;
    }

    private Button btn(String s) {

        Button b =
                new Button(this);

        b.setText(s);
        b.setAllCaps(false);

        return b;
    }

    private TextView section(String s) {

        TextView t =
                txt(
                        s,
                        17,
                        true,
                        Color.rgb(
                                0,
                                72,
                                170
                        )
                );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        p.setMargins(
                0,
                25,
                0,
                8
        );

        t.setLayoutParams(p);

        return t;
    }

    private TextView txt(
            String s,
            int size,
            boolean bold,
            int color
    ) {

        TextView t =
                new TextView(this);

        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        return t;
    }

    private void toast(String s) {

        Toast.makeText(
                this,
                s == null ? "" : s,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {

        if (segmenter != null) {
            segmenter.close();
        }

        super.onDestroy();
    }
}
