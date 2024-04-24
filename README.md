# ProjektNiewidomi
Projekt aplikacji dla niewidomych
Czego używać?
Android studio - jetbrains + google to dobre combo

Co zrobiłem do tej pory
Cały tutorial
https://developer.android.com/codelabs/build-your-first-android-app?authuser=1#8
Dalej nie wiem po co są activity, fragmenty i bindingi - tego muszę się nauczyć - są to części layoutu i fundamenty

Udało mi się zaimportować openCV, ale nie chce współpracować zbytnio.
Krótki tutorial jak to zrobić:
Po pierwsze pobrać openCV ze strony (Na androida)
https://opencv.org/releases/
(najnowsza wersja 4.9)

W programie
File > New > Import Module 
wybieramy folder sdk (tam gdzie wypakowaliśmy openCV) !! Folder dosłownie nazywa się sdk - później nadać nazwę modułu na :openCV - opcjonalne
Klikamy finish

Teraz ta nieoczywista część
Android studio miało konwersje z groovy na kotlina (kotlin używa nawiasów a groovy nie, między innymi, uważajcie na to przy tutorialach!) 
Musicie usunąć autowygenerowany przy imporcie plik settings.graddle - nie ma końcówki .kts - bo jest w groovy - usuńcie go.

W przypadku jakiś błedów typu nie ma biblioteki albo funkcji z com.android.* - spróbuj tego:
1. W settings.graddle.kts (Nazwa Projektu) 
   doklej na dole: include(":openCV")
2. W build.graddle.kts apkowym <- jest projektowy i apkowy, nie pomyl ich
   do dependencies doklej:
   
   implementation("androidx.core:core-ktx:+")
    // Java language implementation
    implementation("androidx.fragment:fragment:$fragment_version")
    // Kotlin
    implementation("androidx.fragment:fragment-ktx:$fragment_version")

   nad dependencies doklej: val fragment_version = "1.6.2"
3. W build.graddle.kts projektowym doklej w pluginach: id("org.jetbrains.kotlin.android") version "1.9.0" apply false
   dodaj też poniżej:
   
  buildscript {
      repositories {
          google()
          mavenCentral()
      }
  }


Na obecną chwilę mam kolosa w piątek, do którego muszę umieć więc moje zdolności przeróbkowe są ograniczone, postaram się coś poczytać. Jeżeli przerobisz ten tutorial wyżej, to będziesz mniej więcej na moim poziomie z czwartku - stąd nie daje kodu z ostatniego spotkania, bo będziesz go prawdopodobnie sam zrobić. Jakieś inne rzeczy i tutoriale z czasem też się tu pojawią.
