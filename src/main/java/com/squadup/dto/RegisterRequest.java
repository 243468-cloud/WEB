package com.squadup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO para el formulario de registro — Vista "Crear Cuenta" */
@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120)
    private String fullName;

    @NotBlank(message = "El apodo es obligatorio")
    @Size(min = 3, max = 50, message = "El apodo debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Confirma tu contraseña")
    private String confirmPassword;
}
