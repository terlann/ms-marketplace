package az.kapitalbank.ms.template.controller;

import az.kapitalbank.ms.template.dto.RequestDto;
import az.kapitalbank.ms.template.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TemplateController {

    @PostMapping("/hello")
    public ResponseEntity<ResponseDto> getHello(@RequestBody RequestDto requestDto) {
        ResponseDto responseDto = new ResponseDto("Hello " + requestDto.getUsername());
        return ResponseEntity.ok(responseDto);
    }
}
