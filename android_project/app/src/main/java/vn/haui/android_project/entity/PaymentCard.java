package vn.haui.android_project.entity;

public class PaymentCard {
    public String cardId; // UID duy nhất cho mỗi thẻ
    public String nameOnCard;
    public String cardNumber;
    public String expirationDate;
    public String cvv;
    public String cardType; // VISA, Mastercard, JCB
    public String last4Digits; // Dùng để hiển thị trên UI (*3282)

    public PaymentCard() {
    }

    public PaymentCard(String cardId, String nameOnCard, String cardNumber, String expirationDate, String cvv, String cardType) {
        this.cardId = cardId;
        this.nameOnCard = nameOnCard;
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.cvv = cvv;
        this.cardType = cardType;
        // Logic đơn giản để lấy 4 số cuối
        this.last4Digits = cardNumber.substring(cardNumber.length() - 4);
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getNameOnCard() {
        return nameOnCard;
    }

    public void setNameOnCard(String nameOnCard) {
        this.nameOnCard = nameOnCard;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getLast4Digits() {
        return last4Digits;
    }

    public void setLast4Digits(String last4Digits) {
        this.last4Digits = last4Digits;
    }
}
