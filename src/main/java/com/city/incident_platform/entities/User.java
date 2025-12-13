package com.city.incident_platform.entities;
import com.city.incident_platform.validations.ValidPassword;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="users")
public class User{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idUser;
	
	@NotBlank(message="Le nom est obligatoire")
	@Size(min = 2 , max = 50)
	private String lastName;
	
	@NotBlank(message = "Le prénom est obligatoire")
	@Size(min = 2 , max = 50)
	private String firstName;
	
	@Email(message = "Email invalide")
	@NotBlank(message = "Email est obligatoire")
	@Column(unique=true)
	private String email;
	
	@NotBlank(message = "Mot de passe est obligatoire")
	@ValidPassword
	private String password;

	
	@Size(max = 20)
    private String telephone;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	private RoleType role;
	
	
	
	private boolean actif = true;
	
	
	
	

}