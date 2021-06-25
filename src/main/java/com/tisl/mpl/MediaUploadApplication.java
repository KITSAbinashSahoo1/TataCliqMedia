package com.tisl.mpl;

import com.tisl.mpl.property.MediaStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		MediaStorageProperties.class
})
public class MediaUploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaUploadApplication.class, args);
	}
}
