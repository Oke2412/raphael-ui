# Thư mục chứa Model RVC (Raphael Voice)

Hãy đặt 2 file model RVC của nhân vật Raphael/Rimuru vào thư mục này:

1. File trọng số mô hình: `raphael.pth`
2. File chỉ mục giọng (Index): `raphael.index` (Tùy chọn nhưng khuyên dùng để giọng chuẩn xác hơn)

### Tùy chỉnh tên file:
Nếu file của bạn có tên khác (ví dụ: `my_voice.pth`), bạn chỉ cần mở file `config/settings.json` và sửa lại dòng:
```json
"rvc": {
  "model_path": "models/rvc/Raphael_Sensei.pth",
  "index_path": "models/rvc/added_IVF160_Flat_nprobe_1_Raphael_Sensei_v2.index"
}
```
Sau đó khởi động lại pipeline.
