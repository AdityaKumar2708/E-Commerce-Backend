
plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.rkind"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {

	// REST APIs
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// Database and ORM
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")

	// Authentication and Authorization
	implementation("org.springframework.boot:spring-boot-starter-security")

	// Request Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Application Monitoring
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")


// JWT Dependencies
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// Razorpay Payment Gateway
	implementation("com.razorpay:razorpay-java:1.4.8")

	implementation("com.cloudinary:cloudinary-http5:2.0.0")

	implementation("org.springframework.boot:spring-boot-starter-actuator")


}

tasks.withType<Test> {
	useJUnitPlatform()
}
