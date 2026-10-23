package vn.offline.anhthepro;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1001;
    private ImageView preview;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUI());
    }

    private View buildUI() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 28, 32, 40);
        root.setBackgroundColor(Color.rgb(244, 249, 255));

        scroll.addView(root);

        TextView title = text(
                "ẢNH THẺ PRO",
                30,
                true,
                Color.rgb(4, 73, 160)
        );
        root.addView(title);

        TextView sub = text(
                "Tạo ảnh thẻ chuyên nghiệp • Hoạt động offline",
                15,
                false,
                Color.DKGRAY
        );
        root.addView(sub);

        preview = new ImageView(this);
        preview.setBackgroundColor(Color.WHITE);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        720
                );

        imageParams.setMargins(0, 28, 0, 20);
        root.addView(preview, imageParams);

        Button choose = button("📷 CHỌN ẢNH TỪ ĐIỆN THOẠI");

        choose.setOnClickListener(v -> {

            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            startActivityForResult(intent, PICK_IMAGE);
        });

        root.addView(choose);

        status = text(
                "Chọn một ảnh chân dung để bắt đầu.",
                14,
                false,
                Color.DKGRAY
        );

        root.addView(status);

        root.addView(section("1. TÁCH & ĐỔI NỀN"));

        root.addView(rowButtons(new String[]{
                "Nền trắng",
                "Nền xanh",
                "Nền đỏ"
        }));

        root.addView(section("2. THAY ÁO"));

        root.addView(rowButtons(new String[]{
                "Sơ mi trắng",
                "Vest nam",
                "Vest nữ"
        }));

        root.addView(section("3. KÍCH THƯỚC ẢNH"));

        root.addView(rowButtons(new String[]{
                "2 × 3 cm",
                "3 × 4 cm",
                "4 × 6 cm"
        }));

        root.addView(section("4. CHỈNH ẢNH"));

        root.addView(rowButtons(new String[]{
                "Căn khuôn mặt",
                "Chỉnh sáng",
                "Làm đẹp"
        }));

        root.addView(section("5. XUẤT ẢNH"));

        root.addView(rowButtons(new String[]{
                "Lưu ảnh",
                "Khổ 10 × 15",
                "Khổ A4"
        }));

        TextView note = text(
                "Ảnh được xử lý trên thiết bị. Không gửi ảnh lên máy chủ.",
                13,
                false,
                Color.GRAY
        );

        LinearLayout.LayoutParams noteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        noteParams.setMargins(0, 28, 0, 0);

        root.addView(note, noteParams);

        return scroll;
    }

    private TextView section(String text) {

        TextView t = text(
                text,
                18,
                true,
                Color.rgb(4, 73, 160)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 30, 0, 10);

        t.setLayoutParams(params);

        return t;
    }

    private LinearLayout rowButtons(String[] labels) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (String label : labels) {

            Button b = button(label);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );

            params.setMargins(3, 3, 3, 3);

            row.addView(b, params);

            b.setOnClickListener(v ->
                    Toast.makeText(
                            this,
                            label,
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        return row;
    }

    private Button button(String text) {

        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);

        return b;
    }

    private TextView text(
            String value,
            int size,
            boolean bold,
            int color
    ) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(null, 1);
        }

        return t;
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

            Uri uri = data.getData();

            preview.setImageURI(uri);

            status.setText(
                    "✓ Đã chọn ảnh. Bây giờ có thể chỉnh nền, áo và kích thước."
            );
        }
    }
}
