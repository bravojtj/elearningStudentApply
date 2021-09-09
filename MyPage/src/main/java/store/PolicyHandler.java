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

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCancelled_ApplyStatusUpdate(@Payload DeliveryCancelled deliveryCancelled){

        if(!deliveryCancelled.validate()) return;

        System.out.println("\n\n##### listener ApplyStatusUpdate : " + deliveryCancelled.toJson() + "\n\n");



        // Sample Logic //

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveried_ApplyStatusUpdate(@Payload Deliveried deliveried){

        if(!deliveried.validate()) return;

        System.out.println("\n\n##### listener ApplyStatusUpdate : " + deliveried.toJson() + "\n\n");



        // Sample Logic //

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCompleted_ApplyStatusUpdate(@Payload PayCompleted payCompleted){

        if(!payCompleted.validate()) return;

        System.out.println("\n\n##### listener ApplyStatusUpdate : " + payCompleted.toJson() + "\n\n");



        // Sample Logic //

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCancelled_ApplyStatusUpdate(@Payload PayCancelled payCancelled){

        if(!payCancelled.validate()) return;

        System.out.println("\n\n##### listener ApplyStatusUpdate : " + payCancelled.toJson() + "\n\n");



        // Sample Logic //

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApplied_ApplyStatusUpdate(@Payload Applied applied){

        if(!applied.validate()) return;

        System.out.println("\n\n##### listener ApplyStatusUpdate : " + applied.toJson() + "\n\n");



        // Sample Logic //

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApplyCancelled_ApplyStatusUpdate(@Payload ApplyCancelled applyCancelled){

        if(!applyCancelled.validate()) return;

        System.out.println("\n\n##### listener ApplyStatusUpdate : " + applyCancelled.toJson() + "\n\n");



        // Sample Logic //

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
