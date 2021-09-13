package store;

import store.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class PolicyHandler{
    @Autowired PayRepository payRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApplyCancelled_PayCancel(@Payload ApplyCancelled applyCancelled){

        if(!applyCancelled.validate()) return;

        System.out.println("\n\n##### listener PayCancel : " + applyCancelled.toJson() + "\n\n");

        // 객체 조회
        Optional<Pay> Optional = payRepository.findById(applyCancelled.getId());

        if( Optional.isPresent()) {
            System.out.println("\n\n##### listener PayCancel Optional ID : " + applyCancelled.getId() + "/BookName" + applyCancelled.getBookName() + "\n\n");
            Pay pay = Optional.get();

            // 객체에 이벤트의 eventDirectValue 를 set 함
            pay.setId(applyCancelled.getId());
            pay.setStudentId(applyCancelled.getStudentId());
            pay.setStudentName(applyCancelled.getStudentName());
            pay.setBookId(applyCancelled.getBookId());
            pay.setBookName(applyCancelled.getBookName());
            pay.setQty(applyCancelled.getQty());
            pay.setAmount(applyCancelled.getAmount());
            pay.setApplyStatus("cancelled");
            pay.setAddress(applyCancelled.getAddress());

            // 레파지 토리에 save
            payRepository.save(pay);
        }

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
