---
description: Wstęp
---

# Cybertech: Dokumentacja aplikacji dla niewidomych

Celem tej gałęzi projektu było wytworzenie aplikacji androidowej w języku java, która na żądanie użytkownika będzie mogła przełączyć się w tryb, w którym możliwe będzie przetwarzanie obrazu z kamery przy pomocy sieci neuronowych i biblioteki opencv Poniżej znajdzie się opis i szczegóły działania aplikacji oraz jej integracji z githubem i opencv.&#x20;

#### Potrzebne oprogramowanie:

* Android studio - [https://developer.android.com/studio](https://developer.android.com/studio) - obecna wersja jest nowsza od tej, którą używam (Iguana)
* Biblioteka opencv - [https://opencv.org](https://opencv.org) - koniecznie wersja na androida
* Android SDK - o ile pamiętam, to Android studio instaluje niektóre rzeczy - takie jak ta - w przypadku pomyłki piszczie na discordzie (Instalacja sdk na androida akurat jest dobrze opisana, więc nie traktuje tego, jako problem)



Ponad to przydatny jest telefon z androidem o wersji większej niż 9.0 oraz kabel USB - wtedy możemy testować kod na własnej maszynie (jest dużo mniej toporne od emulatora, szczególnie w przypadku kamery)



#### Specyfikacja sprzętowa

Wymagamy, aby użytkownik dał zgodę aplikacje na użycie kamery, jeżeli telefon nie jest w taką wyposażony to nie będzie mógł pobrać aplikacji (Do sprawdzenia, zdaje mi się, że widziałem taką informację odnośnie manifestu apki i pozwoleń na kamerę). Telefon musi być też wyposażony w androida 9.0+, co obejmuje dużą część urządzeń (95%+ wg andoid studio).&#x20;

Aplikacja pisana jest w javie, za wygląd odpowiada XML i edytor wizualny.



***



## Jak pobrać projekt do android studio?



1. Stwórz nowy projekt z dowolnym szablonem
2. Wejdź w opcje (trzy poziome kreski między nazwą projektu a ikonką androida, górny pasek, lewa strona)

<figure><img src=".gitbook/assets/image (1).png" alt=""><figcaption><p>Patrz czerwony prostokat :)</p></figcaption></figure>



3. Przejdź na pasku opcji: VCS -> Get from version control...
4. W pole URL skopiuj link z githuba

<figure><img src=".gitbook/assets/image (2).png" alt=""><figcaption></figcaption></figure>

<figure><img src=".gitbook/assets/image (4).png" alt=""><figcaption></figcaption></figure>

5. Klikamy Clone, po zakończeniu - Trust Project. Android studio trochę się buduje, więc zajmie Ci to trochę czasu.
6. Jeżeli wszystko poszło zgodnie z planem powinniście być na masterze - zalecam stworzyć nowego brancha (NIE PUSHUJCIE NA MASTERA), jeżeli planujecie eksperymenty. Jeżeli z jakiegoś powodu nie zmergowałem najnowszej gałęzi na mastera, to pobierzcie tą gałąź, albo się mnie zapytajcie czemu jej nie zmergowałem.

Za chwilę przejdziemy do rzeczy technicznych związanych z aplikacją i kamerą - jeżeli nie jesteś pewien swoich umiejętności/nigdy nie miałeś styczności - zapraszam do sekcji Bibliografia.

***

## Ogólny skład

Apka zawiera dwie aktywności - jedna sprawdza podstawową integracje opencv, druga przetwarza obraz z kamery i poddaje go wyszarzeniu. Pomiędzy apkami przesuwamy się właściwymi guzikami.

#### MainActivity.java

Żeby mieć łatwy dostęp do pól obiektów, które wyświetlamy, trzymamy je wewnątrz klasy, by inne metody mogły je modyfikować. W środku mamy podpięcie listenera do guzika, który ma transformować zdjęcie (wyszarzyć je) przy użyciu opencv po naciśnięciu guzika. Guzik przełączenia się między aktywnością główną a kamery ma podpiętego Intenta zamiast listenera (jest to chyba nowsze, lepsze rozwiązanie). Kiedy guzik zostaje naciśnięty, to ImageListener (paczka ButtonListeners) to wykrywa i wywoła się metoda onClick():

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

Myślę, że pierwsza częśc jest dobrze skomentowana w kodzie. Jeżeli wszystko przebiegło pomyślnie, to z ImageView dostaniemy bitmapę, którą możemy przetwarzać. Aby to zrobić musimy przekonwertować ją na objekt Map z paczki opencv. Funkcje z biblioteki Utils.\* zajmują się konwersją, następnie objekt jest przetwarzany przez Imgproc z opencv i na końcu znowu zamieniany na bitmapę. Finalnie ustawiamy grafikę w imageView na otrzymaną bitmapę.

{% hint style="info" %}
Ważne jest to, że nie zapisujemy bitmapy w pamięci, robimy wszystko w biegu - przyda się nam to przy kamerze
{% endhint %}
