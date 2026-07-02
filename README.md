# Isletme Finans Backend

Bu proje, kucuk bir is yerinin gunluk gelir, gider, kalan bakiye ve devreden artan para bilgisini tutmasi icin hazirlandi.

## Hesap Mantigi

- Her gun icin `gelir` ve `gider` girilir.
- Onceki gunden gelen `artan para`, yeni gunun `openingCarry` degeri olur.
- Gider once bu devreden paradan dusulur.
- Gider hala bitmediyse kalan kisim gunun gelirinden dusulur.
- Gun sonundaki `carryForward`, bir sonraki gunun devreden parasi olur.

Ornek:

- 1. gun: gelir `10000`, gider `5000` -> gun sonu `carryForward = 5000`
- 2. gun: gelir `15000`, gider `8000`
- 2. gunun giderinin ilk `5000` kismi onceki gunden gelen artandan karsilanir
- Kalan `3000` gider, bugunun gelirinden dusulur
- 2. gun sonunda `carryForward = 12000`

## Kullandigi Teknolojiler

- Java 17
- Spring Boot 3.5.7
- Spring Web
- Spring Data JPA
- MySQL

## IntelliJ IDEA ile Calistirma

1. Projeyi IntelliJ IDEA ile ac.
2. Gerekirse `src/main/resources/application.properties` icindeki MySQL bilgilerini kendi sistemine gore duzenle.
3. Maven bagimliliklarini yukle.
4. `KasaTakipApplication` sinifini calistir.

## Varsayilan MySQL Ayari

Uygulama varsayilan olarak su veritabani bilgisini kullanir:

- Database: `isletme_finans`
- Username: `root`
- Password: bos

Istersen bunlari environment variable ile de verebilirsin:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Ana Endpointler

### 1. Gunluk kayit ekleme

`POST /api/daily-records`

```json
{
  "recordDate": "2026-06-29",
  "incomeAmount": 10000,
  "expenseAmount": 5000,
  "description": "Gun sonu kasa kaydi"
}
```

### 2. Gunluk kaydi guncelleme

`PUT /api/daily-records/{id}`

### 3. Tarihe gore gunluk kayit cekme

`GET /api/daily-records/by-date?date=2026-06-29`

### 4. Aylik rapor alma

`GET /api/monthly-reports?month=2026-06`

### 5. Aylik raporu text olarak indirme

`GET /api/monthly-reports/export?month=2026-06`

## Not

Gecmisteki bir gun degistirilirse veya silinirse, o gunden sonraki tum kayitlarin devreden bakiyesi otomatik olarak yeniden hesaplanir.

