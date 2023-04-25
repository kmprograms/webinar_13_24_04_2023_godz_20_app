package app;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

public class App2 {
    /*
        -> Vector API dostarcza ciekawą warstwę abstrakcyjną do obliczeń równoległych
        wspieranych przez nowsze architektury CPU.

        -> Ponieważ różne architektury procesorów występują w różnych wariantach, nie ma prostego
        rozwiązania, które pozwoliłoby wykorzystać możliwości specyficzne dla platformy w
        oprogramowaniu.

        -> Często konieczne jest napisanie kodu w sposób specyficzny dla platformy
        i wykorzystanie specyficznych możliwości platformy, aby uzyskać znakomite korzyści w
        zakresie wydajności.

        -> Vector API stara się umożliwić programistom pisanie oprogramowania
        do równoległego przetwarzania danych w sposób bardzo niezależny od platformy.
        Developer nie musi znać szczegółów docelowego środowiska uruchomieniowego.

        -> CIEKAWY WYKLAD NA TEMAT ROZNYCH MOZLIWOSCI WYKONYWANIA OPERACJI NA DANYCH:
        https://slideplayer.com/slide/5142130/

        Trzeba dodac do siebie elementy tablicy, pierwszy z pierwszym, drugi z drugim itd.

        SISD (Single Instruction, Single Data) - klasyczne podejscie, dwie dane z pierwszej i
        drugiej tablicy sa do siebie dodawane

        SIMD (Single Instruction, Multiple Data) - tablice sa dzielone na kawalki (chunks)
        i jednoczesnie w tym samym czasie dodawane sa poszczegolne elementy z tych "kawalkow"
        W jednym cyklu procesora wykonuje sie kilka dodawan na kilku parach liczb.

        Implementacja SIMD jest rozna dla roznych architektur procesorow (x86, ARM).
        Dzieki Vector API programista moze przestac przejmowac sie tymi roznicami.

        Ciekawy artykul:
        https://medium.com/@Styp/java-18-vector-api-do-we-get-free-speed-up-c4510eda50d2

    */
    static int[] sum(int[] a, int[] b) {
        var c = new int[a.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    // Tworzysz specjalny obiekt, ktory dopasowuje sie do architektury CPU
    // Procesory moga pracowac przykladowo na danych 256 bitowych lub 512 bitowych
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    static int[] vectorSum(int[] a, int[] b) {
        var c = new int[a.length];
        var upperBound = SPECIES.loopBound(a.length);

        var i = 0;

        // Kolejna iteracja petli zwieksza licznik i o SPECIES.length()
        // czyli o ilosc bitow danych (256 / 512) w rejestrach SIMD.
        // Kiedy mamy 512 bitow to przeskakujemy co 16 bo int ma 32 bity
        // wiec 512 / 32 daje nam 16. Przesuwamy sie co 16 elementow, bo tyle
        // w jednym obiegu trafia do 512 bitowego rejestru SIMD.
        // W ten sposob w jednym cyklu mozemy dodawac elementy z zakresu
        // indeksow od a[0] + b[0] do a[15] + b[15], pozniej w kolejnym
        // obiegu z zakresu indeksow od a[16] + b[16] do a[31] + b[31] itd.
        for (; i < upperBound; i += SPECIES.length()) {
            var va = IntVector.fromArray(SPECIES, a, i);
            var vb = IntVector.fromArray(SPECIES, b, i);
            var vc = va.add(vb);
            vc.intoArray(c, i);
        }

        // Elementy pozostale, z koncowych indeksow, ktorych nie udalo sie dopasowac do
        // rejestrow SIMD dodawane sa w sposob klasyczny. Ale odbywa sie to szybko,
        // bo tych elementow nie ma duzo
        for (; i < a.length; i++) {
            c[i] = a[i] + b[i];
        }

        return c;
    }

    public static void main(String[] args) {

    }
}
