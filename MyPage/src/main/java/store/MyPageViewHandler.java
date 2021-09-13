package store;

import store.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyPageViewHandler {


    @Autowired
    private MyPageRepository myPageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenApplied_then_CREATE_1 (@Payload Applied applied) {
        try {

            if (!applied.validate()) return;

            // view 객체 생성
            MyPage myPage = new MyPage();
            // view 객체에 이벤트의 Value 를 set 함

            myPage.setId(applied.getId());
            myPage.setStudentId(applied.getStudentId());
            myPage.setStudentName(applied.getStudentName());
            myPage.setBookId(applied.getBookId());
            myPage.setBookName(applied.getBookName());
            myPage.setBookQty(applied.getQty());
            myPage.setAmount(applied.getAmount());
            myPage.setApplyStatus("completed");
            myPage.setDeliveryAddress(applied.getAddress());
            // view 레파지 토리에 save
            myPageRepository.save(myPage);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenApplyCancelled_then_UPDATE_1(@Payload ApplyCancelled applyCancelled) {
        try {
            if (!applyCancelled.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(applyCancelled.getId());

            if( myPageOptional.isPresent()) {
                 MyPage myPage = myPageOptional.get();
            // view 객체에 이벤트의 eventDirectValue 를 set 함
                 myPage.setApplyStatus("cancelled");
                // view 레파지 토리에 save
                 myPageRepository.save(myPage);
                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayCompleted_then_UPDATE_2(@Payload PayCompleted payCompleted) {
        try {
            if (!payCompleted.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(payCompleted.getId());

            if( myPageOptional.isPresent()) {
                 MyPage myPage = myPageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                 myPage.setApplyStatus("completed");
                // view 레파지 토리에 save
                 myPageRepository.save(myPage);
                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayCancelled_then_UPDATE_3(@Payload PayCancelled payCancelled) {
        try {
            if (!payCancelled.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(payCancelled.getId());

            if( myPageOptional.isPresent()) {
                 MyPage myPage = myPageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                myPage.setApplyStatus("cancelled");
                // view 레파지 토리에 save
                 myPageRepository.save(myPage);
                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveried_then_UPDATE_4(@Payload Deliveried deliveried) {
        try {
            if (!deliveried.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(deliveried.getId());

            if( myPageOptional.isPresent()) {
                 MyPage myPage = myPageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                myPage.setApplyStatus("completed");
                // view 레파지 토리에 save
                 myPageRepository.save(myPage);
                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCancelled_then_UPDATE_5(@Payload DeliveryCancelled deliveryCancelled) {
        try {
            if (!deliveryCancelled.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(deliveryCancelled.getId());

            if( myPageOptional.isPresent()) {
                 MyPage myPage = myPageOptional.get(); 
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                myPage.setApplyStatus("cancelled");
                // view 레파지 토리에 save
                 myPageRepository.save(myPage);
                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

