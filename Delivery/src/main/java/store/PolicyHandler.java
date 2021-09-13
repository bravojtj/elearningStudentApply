package store;

import store.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCompleted_Delivery(@Payload PayCompleted payCompleted){

        if (!payCompleted.validate()) return;
        
        System.out.println("\n\n##### listener Delivery : " + payCompleted.toJson() + "\n\n");

        System.out.println("\n\n##### listener Delivery Optional ID : " + payCompleted.getId() + "/BookName" + payCompleted.getBookName() + "\n\n");

        Delivery delivery = new Delivery();
        delivery.setId(payCompleted.getId());
        delivery.setStudentId(payCompleted.getStudentId());
        delivery.setStudentName(payCompleted.getStudentName());
        delivery.setBookId(payCompleted.getBookId());
        delivery.setBookName(payCompleted.getBookName());
        delivery.setQty(payCompleted.getQty());
        delivery.setAmount(payCompleted.getAmount());
        delivery.setApplyStatus("completed"); 
        delivery.setDeliveryAddress(payCompleted.getAddress());
        deliveryRepository.save(delivery);
    
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
