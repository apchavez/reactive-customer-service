package com.apchavez.customers.domain.model;

import com.apchavez.customers.domain.exception.ClienteDominioInvalidoException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CustomerDomainTest {

    // ── Construcción válida ──────────────────────────────────────────────────

    @Test
    void should_create_customer_with_valid_data() {
        Customer customer = new Customer(1, "Alex", "Prieto", CustomerState.ACTIVE, 30);
        assertThat(customer.nombre()).isEqualTo("Alex");
        assertThat(customer.apellido()).isEqualTo("Prieto");
        assertThat(customer.estado()).isEqualTo(CustomerState.ACTIVE);
        assertThat(customer.edad()).isEqualTo(30);
    }

    @Test
    void should_create_customer_with_null_id() {
        assertDoesNotThrow(() -> new Customer(null, "Alex", "Prieto", CustomerState.INACTIVE, 25));
    }

    // ── Validaciones de nombre ───────────────────────────────────────────────

    @Test
    void should_throw_when_nombre_is_null() {
        assertThatThrownBy(() -> new Customer(null, null, "Prieto", CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class)
                .hasMessage("El nombre no puede estar vacío");
    }

    @Test
    void should_throw_when_nombre_is_blank() {
        assertThatThrownBy(() -> new Customer(null, "   ", "Prieto", CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class)
                .hasMessage("El nombre no puede estar vacío");
    }

    @Test
    void should_pass_when_nombre_has_exactly_150_chars() {
        String maxNombre = "A".repeat(150);
        assertDoesNotThrow(() -> new Customer(null, maxNombre, "Prieto", CustomerState.ACTIVE, 30));
    }

    @Test
    void should_throw_when_nombre_has_151_chars() {
        String overNombre = "A".repeat(151);
        assertThatThrownBy(() -> new Customer(null, overNombre, "Prieto", CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class);
    }

    // ── Validaciones de apellido ─────────────────────────────────────────────

    @Test
    void should_throw_when_apellido_is_null() {
        assertThatThrownBy(() -> new Customer(null, "Alex", null, CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class)
                .hasMessage("El apellido no puede estar vacío");
    }

    @Test
    void should_throw_when_apellido_is_blank() {
        assertThatThrownBy(() -> new Customer(null, "Alex", "\t", CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class)
                .hasMessage("El apellido no puede estar vacío");
    }

    @Test
    void should_pass_when_apellido_has_exactly_150_chars() {
        String maxApellido = "B".repeat(150);
        assertDoesNotThrow(() -> new Customer(null, "Alex", maxApellido, CustomerState.ACTIVE, 30));
    }

    @Test
    void should_throw_when_apellido_has_151_chars() {
        String overApellido = "B".repeat(151);
        assertThatThrownBy(() -> new Customer(null, "Alex", overApellido, CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class);
    }

    // ── Validaciones de edad ─────────────────────────────────────────────────

    @Test
    void should_throw_when_edad_is_null() {
        assertThatThrownBy(() -> new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, null))
                .isInstanceOf(ClienteDominioInvalidoException.class)
                .hasMessage("La edad debe ser mayor que cero");
    }

    @Test
    void should_throw_when_edad_is_zero() {
        assertThatThrownBy(() -> new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, 0))
                .isInstanceOf(ClienteDominioInvalidoException.class);
    }

    @Test
    void should_throw_when_edad_is_negative() {
        assertThatThrownBy(() -> new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, -5))
                .isInstanceOf(ClienteDominioInvalidoException.class);
    }

    @Test
    void should_pass_when_edad_is_exactly_1() {
        assertDoesNotThrow(() -> new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, 1));
    }

    @Test
    void should_pass_when_edad_is_exactly_150() {
        assertDoesNotThrow(() -> new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, 150));
    }

    @Test
    void should_throw_when_edad_is_151() {
        assertThatThrownBy(() -> new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, 151))
                .isInstanceOf(ClienteDominioInvalidoException.class);
    }

    // ── Validaciones de estado ───────────────────────────────────────────────

    @Test
    void should_throw_when_estado_is_null() {
        assertThatThrownBy(() -> new Customer(null, "Alex", "Prieto", null, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class)
                .hasMessage("El estado debe ser 'ACTIVE' o 'INACTIVE'");
    }

    // ── Property-Based Testing: construcción válida ──────────────────────────

    @Property
    void valid_inputs_should_always_succeed(
            @ForAll @AlphaChars @StringLength(min = 1, max = 150) String nombre,
            @ForAll @AlphaChars @StringLength(min = 1, max = 150) String apellido,
            @ForAll CustomerState estado,
            @ForAll @IntRange(min = 1, max = 150) int edad) {
        assertDoesNotThrow(() -> new Customer(null, nombre, apellido, estado, edad));
    }

    // ── Property-Based Testing: nombre en blanco siempre falla ──────────────

    @Provide
    Arbitrary<String> stringsBlancos() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.strings().withChars(' ', '\t', '\n').ofMinLength(1).ofMaxLength(50));
    }

    @Property
    void blank_nombre_should_always_throw(@ForAll("stringsBlancos") String nombre) {
        assertThatThrownBy(() -> new Customer(null, nombre, "Prieto", CustomerState.ACTIVE, 30))
                .isInstanceOf(ClienteDominioInvalidoException.class);
    }
}
