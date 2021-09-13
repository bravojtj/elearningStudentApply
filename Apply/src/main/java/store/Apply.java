package store;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import store.external.Pay;

@Entity
@Table(name="Apply_table")
public class Apply {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String studentId;
    private String studentName;
    private String bookId;
    private String bookName;
    private Integer qty;
    private Double amount;
    private String applyStatus;
    private String address;

    @PostPersist
    public void onPostPersist(){
        System.out.println("################## Apply onPostPersist Applied");
        // configMap 설정 // add by jm
        String cfgServiceType = System.getenv("CFG_SERVICE_TYPE");
        if(cfgServiceType == null) cfgServiceType = "DEVELOP";
        System.out.println("################## CFG_SERVICE_TYPE: " + cfgServiceType);
        
        // kafka에 push
        Applied applied = new Applied();
        BeanUtils.copyProperties(this, applied);
        applied.setApplyStatus("completed");
        // applied.publishAfterCommit(); // modify by jm
        applied.publish(); 
        
        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
        //store.external.Pay pay = new store.external.Pay(); // modify by jm
        Pay pay = new Pay();
        BeanUtils.copyProperties(this, pay);
        
        // feignclient 호출
        ApplyApplication.applicationContext.getBean(store.external.PayService.class).pay(pay);
    }
    
    // add by jm
    @PostUpdate
    public void onPostUpdate(){
        System.out.println("################## Apply onPostUpdate ApplyCancelled");
        // kafka에 push
        ApplyCancelled applyCancelled = new ApplyCancelled();
        BeanUtils.copyProperties(this, applyCancelled);
        applyCancelled.setApplyStatus("cancelled");
        applyCancelled.publishAfterCommit();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public String getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(String applyStatus) {
        this.applyStatus = applyStatus;
    }
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }




}