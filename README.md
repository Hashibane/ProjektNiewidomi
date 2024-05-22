---
description: Wstęp
---

# Cybertech: Dokumentacja aplikacji dla niewidomych

Celem tej gałęzi projektu było wytworzenie aplikacji androidowej w języku java, która na żądanie użytkownika będzie mogła przełączyć się w tryb, w którym możliwe będzie przetwarzanie obrazu z kamery przy pomocy sieci neuronowych i biblioteki opencv Poniżej znajdzie się opis i szczegóły działania aplikacji oraz jej integracji z githubem i opencv.&#x20;

#### Potrzebne oprogramowanie:

* Android studio - [https://developer.android.com/studio](https://developer.android.com/studio) - obecna wersja jest nowsza od tej, którą używam (Iguana)
* Biblioteka opencv - [https://opencv.org](https://opencv.org) - koniecznie wersja na androida
* Android SDK - o ile pamiętam, to Android studio instaluje niektóre rzeczy - takie jak ta - w przypadku pomyłki piszczie na discordzie (Instalacja sdk na androida akurat jest dobrze opisana, więc nie traktuje tego, jako problem)



Ponadto przydatny jest telefon z androidem o wersji większej niż 9.0 oraz kabel USB - wtedy możemy testować kod na własnej maszynie (jest dużo mniej toporne od emulatora, szczególnie w przypadku kamery)



#### Specyfikacja sprzętowa

Wymagamy, aby użytkownik dał zgodę aplikacje na użycie kamery, jeżeli telefon nie jest w taką wyposażony to nie będzie mógł pobrać aplikacji (Do sprawdzenia, zdaje mi się, że widziałem taką informację odnośnie manifestu apki i pozwoleń na kamerę). Telefon musi być też wyposażony w androida 9.0+, co obejmuje dużą część urządzeń (95%+ wg andoid studio).&#x20;

Aplikacja pisana jest w javie, za wygląd odpowiada XML i edytor wizualny.



***



## Jak pobrać projekt do android studio?



1. Stwórz nowy projekt z dowolnym szablonem
2. Wejdź w opcje (trzy poziome kreski między nazwą projektu a ikonką androida, górny pasek, lewa strona)

<figure><img src=".gitbook/assets/image (1).png" alt=""><figcaption><p>Patrz czerwony prostokat :)</p></figcaption></figure>



3. Przejdź na pasku opcji: **VCS -> Get from version control...**
4. W pole URL skopiuj link z githuba

<figure><img src=".gitbook/assets/image (2).png" alt=""><figcaption></figcaption></figure>

<figure><img src=".gitbook/assets/image (4).png" alt=""><figcaption></figcaption></figure>

5. Kliknij Clone, po zakończeniu - Trust Project. Android studio trochę się buduje, więc zajmie Ci to trochę czasu.
6. Jeżeli wszystko poszło zgodnie z planem powinniście być na masterze - zalecam stworzyć nowego brancha **(NIE PUSHUJCIE NA MASTERA)**, jeżeli planujecie eksperymenty. Jeżeli z jakiegoś powodu nie zmergowałem najnowszej gałęzi na mastera, to pobierzcie tą gałąź, albo się mnie zapytajcie czemu jej nie zmergowałem.

Za chwilę przejdziemy do rzeczy technicznych związanych z aplikacją i kamerą - jeżeli nie jesteś pewien swoich umiejętności/nigdy nie miałeś styczności z android studio - zapraszam do sekcji [Bibliografia](./#bibliografia) (link 1).

***

## Ogólny skład

Apka zawiera dwie aktywności - jedna sprawdza podstawową integracje opencv, druga przetwarza obraz z kamery i poddaje go wyszarzeniu. Pomiędzy apkami przesuwamy się właściwymi guzikami.

### MainActivity.java

Żeby mieć łatwy dostęp do pól obiektów, które wyświetlamy, trzymamy je wewnątrz klasy, by inne metody mogły je modyfikować. W środku mamy podpięcie listenera do guzika, który ma transformować zdjęcie (wyszarzyć je) przy użyciu opencv po naciśnięciu guzika. Guzik przełączenia się między aktywnością główną a kamery ma podpiętego Intenta zamiast listenera (jest to chyba nowsze, lepsze rozwiązanie). Kiedy guzik zostaje naciśnięty, to ImageListener (paczka ButtonListeners) to wykrywa i wywoła się metoda `onClick()`:

```
public class ImageChangeListener implements View.OnClickListener {
    ...
    @Override
    public void onClick(View v) {
        Bitmap image = null;
        //PNG, JPGS .. are bitmap drawables! DONT DO XMLS, Vector graphics etc
        if (imageView.getDrawable() instanceof BitmapDrawable)
        {
            image = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        }
        //validity check
        if (image == null)
            return;
            
        Mat matObject = new Mat();
        Utils.bitmapToMat(image, matObject);

        Mat destination = new Mat();

        Imgproc.cvtColor(matObject, destination, Imgproc.COLOR_RGB2GRAY);

        Utils.matToBitmap(destination, image);
        image = Bitmap.createScaledBitmap(image, 600, 600, true);
        imageView.setImageBitmap(image);
    }
}
```

Myślę, że pierwsza częśc jest dobrze skomentowana w kodzie. Jeżeli wszystko przebiegło pomyślnie, to z `ImageView` dostaniemy bitmapę, którą możemy przetwarzać. Aby to zrobić musimy przekonwertować ją na objekt `Map`z paczki opencv. Funkcje z biblioteki Utils.\* zajmują się konwersją, następnie objekt jest przetwarzany przez Imgproc z opencv i na końcu znowu zamieniany na bitmapę. Finalnie ustawiamy grafikę w `imageView` na otrzymaną bitmapę.

{% hint style="info" %}
Ważne jest to, że nie zapisujemy bitmapy w pamięci, robimy wszystko "w biegu" - przyda się nam to przy kamerze
{% endhint %}



### CameraActivity.java

#### Otwarcie kamery

Tutaj zaczynają się trudniejsze rzeczy - przede wszystkim, w tej aktywności cały ekran jest poświęcony `textureView`, który będzie wyświetlał obraz z kamery - jak to zrobić?

Po pierwsze musimy do `textureView`podłączyć listenera, który będzie nasłuchiwał na event `onSurfaceTextureAvailable(...)`(tutaj jako klasa wewnętrzna aktywności):

```
private class SurfaceTextureListener implements TextureView.SurfaceTextureListener
    {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {}
    }
```

Teraz przejdziemy do funkcji `setupCamera(...)` - `CameraManager`to zarządca wszystkich naszych kamer i możemy dostać objekt odpowiadający kamerze za jego pośrednictwem. Kiedy dostaniemy instancje managera, skanujemy listę ID kamer i szukamy pierwszej takiej, która jest skierowana do tyłu (ignorujemy te do przodu - czyli z właściwością `LENS_FACING_FRONT`). Później w zależności od orientacji urządzenia wybieramy rozmiar najlepszy rozmiar dla naszego `textureView`.

```
private void setupCamera(int width, int height)
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String i : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(i);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                //checks for lanscape / portrait mode swap
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation)
                {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                cameraID = i;
                return;
            }
        }
        catch (CameraAccessException | NullPointerException e)
        {
            //TODO: LOGGING
        }
    }
```

Teraz przejdziemy do `connectCamera()`

```
private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //PERMISSION CHECK
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)  {
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                cameraManager.openCamera(cameraID, cameraDeviceStateCallback, backgroundHandler);
            }
            else  {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    Toast.makeText(this, "Video app reqires access to camera", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }

        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }
```

Najpierw sprawdzamy, czy mamy zgodę użytkownika - jeżeli nie to o nią prosimy, następnie otwieramy kamerę przez `cameraManagera`. Od tego momentu możemy się komunikować z naszą kamerą! Robimy to przez listener `CameraDeviceCallback`(klasa wewnętrzna aktywności) - tu obsługujemy eventy związane z kamerą. Tu ustawiamy obecnie obsługiwaną kamerę na tą otrzymaną z eventa `onOpened(...)`. Oprócz tego tutaj obsługujemy zamykanie kamery (jest to ważne, inaczej inne aplikacje po zamknięciu naszej nie mogą korzystać z kamery).

```
private class CameraDeviceCallback extends CameraDevice.StateCallback
{
    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        cameraDevice = camera;
        startPreview();
        //Toast.makeText(getApplicationContext(), "Camera connection established", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        cameraDevice.close();
        cameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        cameraDevice.close();
        cameraDevice = null;
    }
}
```

Tu też rozpoczynamy transmisję na textureView - taka transmisja nazywa się preview.

Teraz przejdziemy do `startPreview()`:

Najpierw z `textureView`pobieramy `surfaceTexture`- na to będziemy rzutować nasz przetworzony obraz - tylko ustawiamy właściwy rozmiar - w przypadku użycia bez przetwarzania obrazu - dodajemy tą powierzchnie jako cel do rzutowania dla `captureRequestBuildera`i sesji kamery (o tym później). Później pobieramy instancje `ImageReadera`- za pośrednictwem tej klasy będziemy przetwarzać zdjęcia - mamy tu m. in. wybór formatu otrzymywanych klatek z kamery oraz ich ilość maksymalną.

Teraz musimy zbudować rządanie rozpoczęcia sesji dla kamery - po to jest nam `RequestBuilder` - dajemy mu informacje, że chcemy używać funkcjonalności `PREVIEW` i dajemy informacje, na co będziemy rzutować obraz otrzymywany z kamery - pośrednikiem będzie `imageReader`, więc nie będziemy rzutować na `surfaceTexture`.

```
private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        }
        else {
            Toast.makeText(getApplicationContext(), "SurfaceTexture has not been properly initalized", Toast.LENGTH_SHORT).show();
            return;
        }

        imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 5);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

        try {

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_MONO);
            
            //Session setup
            //previewSurface
            cameraDevice.createCaptureSession(Arrays.asList( imageReader.getSurface()), new CameraSessionStateCallback(), null);
        } catch (CameraAccessException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }
```



### Przetwarzanie obrazu

Przejdziemy teraz do klasy wewnętrznej `OnImageAvailableListener`- ona otrzymuje eventy związane ze zdjęciami, które kamera zrobiła. Gdy kamera zrobi zdjęcia otrzymujemy event `onImageAvailable()`:

```
private class OnImageAvailableListener implements ImageReader.OnImageAvailableListener {
    Matrix rotationMatrix = new Matrix();
    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image != null) {
                byte[] jpegBytes = ImageUtilClass.toJpeg(image).toByteArray();
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
                Mat matObject = new Mat();
                Utils.bitmapToMat(imageBitmap, matObject);

                Mat destination = new Mat();

                Imgproc.cvtColor(matObject, destination, Imgproc.COLOR_RGB2GRAY);

                Utils.matToBitmap(destination, imageBitmap);
                //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, textureView.getHeight(), textureView.getWidth(), true);
                int rotation = 0;
                try {
                    rotation = sensorToDeviceRotation(((CameraManager) getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics(cameraID), getWindowManager().getDefaultDisplay().getRotation());
                    System.out.println(rotation);
                }
                catch (CameraAccessException e)
                {
                    Log.e("DRAW BITMAP ON TEXTUREVIEW", "Unable to draw");
                }

                rotationMatrix.reset();
                rotationMatrix.postRotate(rotation + 180);
                imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), rotationMatrix, true);
                //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, textureView.getHeight(), textureView.getWidth(), true);
                final Bitmap bitmap = imageBitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        drawBitmapOnTextureView(bitmap);
                    }
                });


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }
```

Większość tego kodu jest analogiczna do button listenera z aktywności **ActivityMain.java** - zamieniamy bitmape na obiekt `Map` i z powrotem, na końcu rysujemy na `textureView` przy pomocy innego wątku, by nie zabierać czasu. Za konwersje formatu odpowiada klasa `ImageUtilClass`.

```
public final class ImageUtilClass {


    public static ByteArrayOutputStream toJpeg(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Order of U/V channel guaranteed, read more:
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        yuvImage.compressToJpeg(new Rect(0,0, width, height), 100, out);
        Log.e("SUCESSFUL JPEG CONVERSION","ZUCCESS");
        return out;
    }

}
```



Niektóre funkcje nie zostały omówione w dokumentacji - uważam, że ich wytłumaczenia nie są szczególnie potrzebne.

## Plany na przyszłość



1. Naprawić bugi związane z obrotem obrazu
2. Fullscreen mode (to chyba akurat powinno być proste - w najbliższym czasie będzie)
3. Usprawnić szybkość przetwarzania obrazu
4. Zastanowić się jakie UI zrobić - raczej co do tego będziemy jeszcze mówić na spotkaniu koła
5. Poeksperymentować z innymi formatami



## Bibliografia

[https://developer.android.com/codelabs/build-your-first-android-app#0](https://developer.android.com/codelabs/build-your-first-android-app#0) - podstawy android studio - dobrze jest tu zacząć, aby rozumieć co się dzieje, nawet jeżeli nie zamierzacie się wgłębiać w tajniki - pozwoli się to wam efektywnie poruszać po edytorze - nie zajmuje dużo czasu - około godzina na wszystko. Z tego co pamiętam wszystkie **potrzebne** rzeczy około 30 min - chyba jest tam m. in. jak testować kod na telefonie i emulatorze - przydatne, jeżeli chcecie uruchomić.&#x20;

[https://www.javatpoint.com/android-core-building-blocks](https://www.javatpoint.com/android-core-building-blocks) - informacje o strukturze aplikacji i poradniki na temat podstaw w Javie (Bardzo przydatny na początek programowania aplikacji w android studio)

[https://www.youtube.com/watch?v=CuvVpsFc77w\&list=PL9jCwTXYWjDIHNEGtsRdCTk79I9-95TbJ](https://www.youtube.com/watch?v=CuvVpsFc77w\&list=PL9jCwTXYWjDIHNEGtsRdCTk79I9-95TbJ) - Camera2 api jest dosyć trudne - ale umożliwia efektywne używanie kamery na niskim poziomie - ja posługiwałem się tym poradnikiem, moim zdaniem jest całkiem spoko.

[https://developer.android.com/media/camera/camera2/capture-sessions-requests](https://developer.android.com/media/camera/camera2/capture-sessions-requests) - kompletna dokumenacja camera2 api - tylko dla wytrwałych - początkową stronę warto przeczytać.
