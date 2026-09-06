package org.gms.soloMapling.ArtificialPlayer.BotTradeSystem;

import org.gms.client.Character;
import org.gms.client.inventory.Equip;
import org.gms.client.inventory.Item;
import org.gms.server.Trade;
import org.gms.soloMapling.ArtificialPlayer.BotBlockList;
import org.gms.soloMapling.ArtificialPlayer.BotSM;
import org.gms.soloMapling.server.BotTiming;

import java.util.List;

import static org.gms.soloMapling.ArtificialPlayer.BotCommandsPack.SocialCommands.BotEmote;
import static org.gms.soloMapling.ArtificialPlayer.BotCommandsPack.SocialCommands.BotSpeak;
import static org.gms.soloMapling.ArtificialPlayer.BotHelpers.convertItemIdToName;
import static org.gms.soloMapling.ArtificialPlayer.BotTradeSystem.BotTradeCommands.getTradePartnerCharacter;
import static org.gms.soloMapling.DebugUtilities.debugprint;
import static org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand;
import static org.gms.soloMapling.itemPool.ItemUtilities.isEquip;

public class BotTradeSM {

    public enum TradeState {
        INITIALIZE,
        WAITING_RESPONSE,
        RESPONDING,
        AWAITING_CONFIRMATION,
        CONFIRMING,
        CONFIRMED_LOCKED,
        CLEANUP,
        COMPLETED,
        TIMED_OUT,
        DECLINE,
    }

    public enum TradeMode {
        SELLING,
        BUYING,
        NULL
    }

    private BotSM parentSM;
    private boolean offerAccepted = false;
    private boolean tradeComplete = false;
    private TradeMode tradeMode = TradeMode.NULL;
    private Trade.TradeResult lastTradeResult = null;


    protected BotTradeSM.TradeState tradeState;

    private long startTime;
    private long endTime;
    private long timeoutSeconds = 60;

    public BotTradeSM(BotSM parent) {
        this(parent, TradeMode.SELLING);
    }

    public BotTradeSM(BotSM parent, TradeMode mode) {
        this.parentSM = parent;
        this.tradeMode = mode;
        setTradeState(TradeState.INITIALIZE);
        setTradeStartTime();
    }

    protected void setTradeStartTime() {
        startTime = System.currentTimeMillis();
    }

    private BotSM getParent() {
        return this.parentSM;
    }

    private Character getChr() {
        return getParent().getChr();
    }

    public TradeMode getTradeMode() {
        return this.tradeMode;
    }

    protected void setTradeState(BotTradeSM.TradeState tradeState) {
        this.tradeState = tradeState;
    }

    protected BotTradeSM.TradeState getTradeState() {
        return this.tradeState;
    }

    protected boolean calculateTimeOut() {
        endTime = startTime + (timeoutSeconds * 1000);
        if (System.currentTimeMillis() > endTime) {
            debugprint("Timed out");
            return true;
        }
        return false;
    }

    protected boolean isSelling() {
        return getTradeMode() == TradeMode.SELLING;
    }

    protected boolean isBuying() {
        return getTradeMode() == TradeMode.BUYING;
    }

    public void update() {
        switch (getTradeState()) {
            case INITIALIZE:
                startTradeCallback();
                if (isSelling()) {
                    // Selling flow: Post items first, then wait for response
                    if (postItemsForSale()) {
                        setTradeState(TradeState.RESPONDING);
                    } else {
                        setTradeState(TradeState.WAITING_RESPONSE);
                    }
                } else if (isBuying()) {
                    // Buying flow: Show what we're looking for first
                    BotTradeCommands.writeTradeChat(getChr(), generateWantsMessageString());
                    setTradeState(TradeState.WAITING_RESPONSE);
                } else {
                    //null
                    BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "我现在没有什么想要交易的" : "I don't have anything at the moment");
                    setTradeState(TradeState.WAITING_RESPONSE);
                }
                break;
            case RESPONDING:
                if (isSelling()) {
                    // Selling mode: Tell what we want in exchange
                    setTradeStartTime();
                    BotTradeCommands.writeTradeChat(getChr(), generateWantsMessageString());
                } else if (isBuying() && isCorrectItemOffered()) {
                    // Buying mode: Respond with mesos/items we're offering
                    postMesosForBuying();
                    setTradeState(TradeState.CONFIRMING);
                    break;
                }
                setTradeState(TradeState.WAITING_RESPONSE);
                break;
            case WAITING_RESPONSE:
                if (isSelling()) {
                    // Selling flow: Wait for partner to offer what we want
                    if (!isSufficientToAccept() && BotTradeCommands.isPartnerLocked(getChr())) {
                        setTradeState(TradeState.DECLINE);
                        break;
                    }
                    if (isSufficientToAccept()) {
                        setTradeState(TradeState.CONFIRMING);
                        break;
                    }
                } else if (isBuying()) {
                    // Buying flow: Wait for partner to offer what we want to buy
                    if (isCorrectItemOffered()) {
                        debugprint("good item!");
                        setTradeState(TradeState.RESPONDING);
                        break;
                    }
                    if (BotTradeCommands.isPartnerLocked(getChr()) && !isCorrectItemOffered()) {
                        setTradeState(TradeState.DECLINE);
                        break;
                    }
                }
                break;
            case CONFIRMING:
                setTradeStartTime();
                setOfferAccepted();
                BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "交易没问题，确认了！" : "trade looks good to go!");
                BotTradeCommands.confirmTrade(getChr());
                setTradeState(TradeState.CONFIRMED_LOCKED);
                break;
            case CONFIRMED_LOCKED:
                debugprint("CONFIRMED LOCKED");
                if (getParent().getTradeHandler().verifyTradePartner()) {
                    break;
                }
                setTradeState(TradeState.CLEANUP);
                break;

            case CLEANUP:
                getParent().getTradeInventory().resetItemsForSale();
                getParent().getTradeWants().resetTradeWants();
                getParent().setTradeMode(TradeMode.NULL);

                if (lastTradeResult != Trade.TradeResult.SUCCESSFUL) {
                    BotEmote(getChr(), 4);
                    BotSpeak(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "为什么取消交易了？" : "Why did you decline?");
                } else {
                    BotEmote(getChr(), 2);
                    BotSpeak(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "多谢，合作愉快！" : "Thank you!");
                    getParent().setLastTradeResult(Trade.TradeResult.SUCCESSFUL);
                }
                getParent().waitFor(2000); // farewell beat before COMPLETED ticks
                lastTradeResult = null;
                setTradeState(TradeState.COMPLETED);
                break;
            case COMPLETED:
                debugprint("COMPLETED");
                setTradeCompleted();
                break;
            case TIMED_OUT:
                BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "交易超时！" : "Timed Out!");
                // decline (closes the trade window) lands 2s after the message; hold
                // the bot slightly past it so nothing runs while the window is open
                BotTiming.after(2000, () -> BotTradeCommands.declineTradeInvite(getChr()));
                getParent().waitFor(2500);
                setTradeCompleted();
                setTradeState(TradeState.COMPLETED);
                break;
            case DECLINE:
                declineTradeOffer();
                setTradeState(TradeState.COMPLETED);
                break;
            default:
                throw new IllegalStateException("Unexpected state: " + tradeState);
        }
        if (calculateTimeOut()) {
            setTradeState(TradeState.TIMED_OUT);
        }
    }

    protected boolean isSufficientToAccept() {
        if (getParent().getTradeWants().verifyTrade(BotTradeCommands.readPartnerMeso(getChr()),
                BotTradeCommands.getPartnersItems(getChr()))) {
            return true;
        }
        return false;
    }

    protected boolean isCorrectItemOffered() {
        List<Item> partnerItems = BotTradeCommands.getPartnersItems(getChr());
        return getParent().getTradeWants().verifySufficientItems(partnerItems);
    }

    protected void postMesosForBuying() {
        int mesoOffering = getParent().getTradeWants().getMesoOffering();
        if (mesoOffering > 0) {
            BotTradeCommands.setMeso(getChr(), mesoOffering);
            BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ?
                    ("这是给你道具的 " + formatPriceToShorthand(mesoOffering) + " 金币！") :
                    ("Here's " + mesoOffering + " mesos for your item!"));
        }

        // Also offer any items we might be exchanging
//        List<Item> itemsOffering = getParent().getTradeWants().getItemsOffering();
//        if (itemsOffering != null && !itemsOffering.isEmpty()) {
//            for (Item item : itemsOffering) {
//                if (item instanceof Equip) {
//                    BotTradeCommands.addEquipToTrade(getChr(), (Equip)item, 1);
//                } else {
//                    BotTradeCommands.addItemToTrade(getChr(), item, item.getQuantity());
//                }
//            }
//        }

        // Move to confirming if we're satisfied with the trade

    }

    protected boolean waitForPartnerMesoOffer(int mesoOffer) {
        if (BotTradeCommands.readPartnerMeso(getChr()) >= mesoOffer) {
            return true;
        }
        return false;
    }

    public void setTradeCompleted() {
        tradeComplete = true;
    }

    public boolean isTradeComplete() {
        return tradeComplete;
    }

    public void setOfferAccepted() {
        offerAccepted = true;
    }

    public boolean isOfferAccepted() {
        return offerAccepted;
    }

    protected boolean postItemsForSale() {
        Item itemForSale = getParent().getTradeInventory().getMainItemForSale();
        if (itemForSale == null) {
            BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "不好意思，我现在没有出售任何东西" : "I don't have anything for sale currently, sorry");
            return false;
        }

        if (isEquip(itemForSale)) {
            Equip eqForSale = (Equip) getParent().getTradeInventory().getMainItemForSale();
            BotTradeCommands.addEquipToTrade(getChr(), eqForSale, 1);
            BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "这是我要出售的，看看吧！" : "Here is what I've got. check it out!");
            return true;
        } else {
            BotTradeCommands.addItemToTrade(getChr(), itemForSale.getItemId(), 1, 1);
            BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "这是我要出售的，看看吧！" : "Here is what I've got. check it out!");
            return true;
        }
    }

    /**
     * Generates a simple message describing what the user wants in a trade.
     *
     * @return A string that describes the mesos and/or items wanted.
     */
    protected String generateWantsMessageString() {
        int mesoWanted = getParent().getTradeWants().getMesoWanted();
        List<ItemQuantity> itemsWanted = getParent().getTradeWants().getItemsWanted();
        boolean isZh = org.gms.soloMapling.server.SoloMaplingI18n.isChinese();
        StringBuilder wantsMessage = new StringBuilder(isZh ? "想要 " : "I want ");

        // Meso part
        if (mesoWanted > 0) {
//            wantsMessage.append(mesoWanted).append(" mesos");
            wantsMessage.append(formatPriceToShorthand(mesoWanted));
            if (isZh) {
                wantsMessage.append("金币");
            }
            // Add "and" if there are also items
            if (itemsWanted != null && !itemsWanted.isEmpty()) {
                wantsMessage.append(isZh ? " 和 " : " and ");
            }
        }

        // Items part
        if (itemsWanted != null && !itemsWanted.isEmpty()) {
            if (itemsWanted.size() == 1) {
                ItemQuantity item = itemsWanted.get(0);
                String itemName = convertItemIdToName(item.getItemId());
                if (item.getQuantity() > 1) {
                    wantsMessage.append(item.getQuantity()).append(isZh ? "个 " : "x ").append(itemName);
                } else {
                    wantsMessage.append(itemName);
                }
            } else {
                for (int i = 0; i < itemsWanted.size(); i++) {
                    ItemQuantity item = itemsWanted.get(i);
                    String itemName = convertItemIdToName(item.getItemId());

                    if (item.getQuantity() > 1) {
                        wantsMessage.append(item.getQuantity()).append(isZh ? "个 " : "x ").append(itemName);
                    } else {
                        wantsMessage.append(itemName);
                    }

                    if (i < itemsWanted.size() - 1) {
                        wantsMessage.append(isZh ? "、" : ", ");
                    }
                }
            }
        }

        // Nothing wanted
        if (mesoWanted == 0 && (itemsWanted == null || itemsWanted.isEmpty())) {
            wantsMessage.append(isZh ? "随便给点什么都行" : "nothing specific");
        }

        return wantsMessage.toString();
    }

    protected void declineTradeOffer() {
        BotBlockList.getInstance().addToBlockList(getChr().getId(), getTradePartnerCharacter(getChr()).getId());
        BotTradeCommands.writeTradeChat(getChr(), org.gms.soloMapling.server.SoloMaplingI18n.isChinese() ? "算了，不换了。再见。" : "Nah I'm good. Good bye.");
        BotTiming.after(2000, () -> BotTradeCommands.declineTradeInvite(getChr()));
        getParent().waitFor(2500); // hold until the delayed decline lands
    }

    public void onTradeSuccess() {
        lastTradeResult = Trade.TradeResult.SUCCESSFUL;
    }

    public void startTradeCallback() {
        Trade trade = getChr().getTrade(); /* get or create the Trade object */
        getParent().setLastTradedCharacter(BotTradeCommands.getTradePartnerCharacter(getChr()));
        // IMPORTANT: Set the callback IMMEDIATELY after getting the Trade object
        trade.setTradeResultCallback(result -> {
            if (result == Trade.TradeResult.SUCCESSFUL) {
                debugprint("**************Successful trade!");
                onTradeSuccess();
            }
        });
    }

}
