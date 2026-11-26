package com.example.thymeleaf.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security By Design - Zadanie 3 / Zadanie 1
 * Rozszerzony scenariusz testow jednostkowych dla klasy com.example.thymeleaf.entity.Student.
 *
 * Zakres (wg README):
 *   1. Testy poprawnych danych
 *   2. Testy niepoprawnych danych
 *   3. Testy prob wstrzykniecia kodu SQL i JavaScript
 *   4. Testy ekstremalne
 *
 * UWAGA: encja Student jest zwyklym POJO (Lombok @Getter/@Setter) i nie zawiera
 * zadnej walidacji. Testy z grup 2-4 dokumentuja ten fakt: pokazuja, ze obiekt
 * przyjmuje dane niepoprawne/zlosliwe bez sanityzacji. Zgodnie z trescia zadania
 * nie poprawiamy definicji klasy - celem jest przygotowany zestaw testow.
 */
class StudentTest {

    private Student validStudent() {
        Student s = new Student();
        s.setId("11111111-1111-1111-1111-111111111111");
        s.setName("Jan Kowalski");
        s.setEmail("jan.kowalski@example.com");
        s.setBirthday(LocalDate.of(1998, 5, 20));
        return s;
    }

    // ------------------------------------------------------------------
    // 1. TESTY POPRAWNYCH DANYCH
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("1. Poprawne dane")
    class ValidData {

        @Test
        @DisplayName("Settery/gettery przechowuja poprawne wartosci")
        void shouldStoreValidValues() {
            Student s = validStudent();
            assertEquals("Jan Kowalski", s.getName());
            assertEquals("jan.kowalski@example.com", s.getEmail());
            assertEquals(LocalDate.of(1998, 5, 20), s.getBirthday());
            assertEquals("11111111-1111-1111-1111-111111111111", s.getId());
        }

        @Test
        @DisplayName("Powiazanie z Address dziala w obie strony")
        void shouldLinkAddress() {
            Student s = validStudent();
            Address a = new Address();
            a.setCity("Warszawa");
            a.setStreet("Marszalkowska");
            a.setZipCode("00-001");
            s.setAddress(a);
            assertNotNull(s.getAddress());
            assertEquals("Warszawa", s.getAddress().getCity());
        }

        @Test
        @DisplayName("equals/hashCode oparte wylacznie o id")
        void equalsByIdOnly() {
            Student a = validStudent();
            Student b = new Student();
            b.setId(a.getId());
            b.setName("Inne Imie");           // rozne pozostale pola
            b.setEmail("other@example.com");
            assertEquals(a, b, "Obiekty o tym samym id maja byc rowne");
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Rozne id => obiekty nierowne")
        void notEqualForDifferentId() {
            Student a = validStudent();
            Student b = validStudent();
            b.setId("22222222-2222-2222-2222-222222222222");
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("toString zawiera kluczowe pola")
        void toStringContainsFields() {
            String txt = validStudent().toString();
            assertTrue(txt.contains("Jan Kowalski"));
            assertTrue(txt.contains("jan.kowalski@example.com"));
        }
    }

    // ------------------------------------------------------------------
    // 2. TESTY NIEPOPRAWNYCH DANYCH
    // (encja nie waliduje - testy dokumentuja brak walidacji)
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("2. Niepoprawne dane")
    class InvalidData {

        @Test
        @DisplayName("Akceptuje null jako imie (BRAK WALIDACJI - oczekiwane odrzucenie)")
        void acceptsNullName() {
            Student s = validStudent();
            s.setName(null);
            // Bezpieczna implementacja powinna odrzucic null - tu jest akceptowany:
            assertNull(s.getName());
        }

        @Test
        @DisplayName("Akceptuje pusty email (BRAK WALIDACJI)")
        void acceptsEmptyEmail() {
            Student s = validStudent();
            s.setEmail("");
            assertEquals("", s.getEmail());
        }

        @Test
        @DisplayName("Akceptuje email bez znaku @ (BRAK WALIDACJI formatu)")
        void acceptsMalformedEmail() {
            Student s = validStudent();
            s.setEmail("to-nie-jest-email");
            assertFalse(s.getEmail().contains("@"),
                    "Encja przyjmuje email o niepoprawnym formacie");
        }

        @Test
        @DisplayName("Akceptuje date urodzenia w przyszlosci (BRAK WALIDACJI)")
        void acceptsFutureBirthday() {
            Student s = validStudent();
            LocalDate future = LocalDate.now().plusYears(50);
            s.setBirthday(future);
            assertTrue(s.getBirthday().isAfter(LocalDate.now()));
        }

        @Test
        @DisplayName("Akceptuje imie zlozone tylko z bialych znakow (BRAK WALIDACJI)")
        void acceptsBlankName() {
            Student s = validStudent();
            s.setName("     ");
            assertTrue(s.getName().trim().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 3. TESTY WSTRZYKNIECIA KODU (SQL / JavaScript)
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("3. Wstrzykniecie SQL / JavaScript")
    class Injection {

        @Test
        @DisplayName("SQL injection w polu name jest przechowywane doslownie (brak sanityzacji)")
        void sqlInjectionInName() {
            Student s = validStudent();
            String payload = "Robert'); DROP TABLE student;--";
            s.setName(payload);
            assertEquals(payload, s.getName(),
                    "Encja nie sanityzuje danych - ladunek SQL zapisany bez zmian");
        }

        @Test
        @DisplayName("SQL injection typu OR 1=1 w polu email")
        void sqlInjectionInEmail() {
            Student s = validStudent();
            String payload = "' OR '1'='1";
            s.setEmail(payload);
            assertEquals(payload, s.getEmail());
        }

        @Test
        @DisplayName("XSS/JavaScript w polu name jest przechowywane doslownie")
        void xssInName() {
            Student s = validStudent();
            String payload = "<script>alert('xss')</script>";
            s.setName(payload);
            assertEquals(payload, s.getName());
            assertTrue(s.getName().contains("<script>"),
                    "Encja nie koduje/escapuje znacznikow HTML");
        }

        @Test
        @DisplayName("Event-handler JS w polu name jest przechowywane doslownie")
        void xssEventHandler() {
            Student s = validStudent();
            String payload = "\"><img src=x onerror=alert(1)>";
            s.setName(payload);
            assertEquals(payload, s.getName());
        }

        @Test
        @DisplayName("Ladunek wstrzykniecia widoczny w toString (ryzyko log injection)")
        void injectionVisibleInToString() {
            Student s = validStudent();
            s.setName("'; DELETE FROM student; --");
            assertTrue(s.toString().contains("DELETE FROM student"));
        }
    }

    // ------------------------------------------------------------------
    // 4. TESTY EKSTREMALNE
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("4. Przypadki ekstremalne")
    class Extreme {

        @Test
        @DisplayName("Bardzo dlugie imie (100 000 znakow) jest akceptowane")
        void veryLongName() {
            Student s = validStudent();
            String longName = "A".repeat(100_000);
            s.setName(longName);
            assertEquals(100_000, s.getName().length());
        }

        @Test
        @DisplayName("Pusty string jako imie")
        void emptyName() {
            Student s = validStudent();
            s.setName("");
            assertEquals("", s.getName());
        }

        @Test
        @DisplayName("Znaki Unicode i emoji w imieniu")
        void unicodeAndEmoji() {
            Student s = validStudent();
            String name = "Zazolc gesla jazn óęą 😀🔥";
            s.setName(name);
            assertEquals(name, s.getName());
        }

        @Test
        @DisplayName("Graniczne wartosci LocalDate (MIN/MAX)")
        void extremeDates() {
            Student s = validStudent();
            s.setBirthday(LocalDate.MIN);
            assertEquals(LocalDate.MIN, s.getBirthday());
            s.setBirthday(LocalDate.MAX);
            assertEquals(LocalDate.MAX, s.getBirthday());
        }

        @Test
        @DisplayName("Null id - equals nie rzuca wyjatku")
        void nullIdEquals() {
            Student a = new Student();
            Student b = new Student();
            assertDoesNotThrow(() -> a.equals(b));
            assertEquals(a, b, "Dwa obiekty z id=null sa rowne wg Lombok");
        }

        @Test
        @DisplayName("Znak NUL i znaki kontrolne w imieniu")
        void controlCharacters() {
            Student s = validStudent();
            String name = "abc\u0000def\u0007\u001b";
            s.setName(name);
            assertEquals(name, s.getName());
        }
    }
}
