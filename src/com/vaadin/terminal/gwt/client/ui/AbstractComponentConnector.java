/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.ComponentConnector;
import com.vaadin.terminal.gwt.client.ComponentContainerConnector;
import com.vaadin.terminal.gwt.client.ComponentState;
import com.vaadin.terminal.gwt.client.ConnectorMap;
import com.vaadin.terminal.gwt.client.LayoutManager;
import com.vaadin.terminal.gwt.client.TooltipInfo;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.communication.ServerRpc;
import com.vaadin.terminal.gwt.client.communication.ServerRpc.InitializableClientToServerRpc;
import com.vaadin.terminal.gwt.client.communication.SharedState;

public abstract class AbstractComponentConnector extends AbstractConnector
        implements ComponentConnector {

    private ComponentContainerConnector parent;

    // Generic UIDL parameter names, to be moved to shared state.
    // Attributes are here mainly if they apply to all paintable widgets or
    // affect captions - otherwise, they are in the relevant subclasses.
    // For e.g. item or context specific attributes, subclasses may use separate
    // constants, which may refer to these.
    // Not all references to the string literals have been converted to use
    // these!
    public static final String ATTRIBUTE_ICON = "icon";
    public static final String ATTRIBUTE_REQUIRED = "required";
    public static final String ATTRIBUTE_ERROR = "error";
    public static final String ATTRIBUTE_HIDEERRORS = "hideErrors";

    private Widget widget;

    /* State variables */
    private boolean enabled = true;
    private boolean visible = true;

    // shared state from the server to the client
    private ComponentState state;

    private String declaredWidth = "";
    private String declaredHeight = "";

    /**
     * Default constructor
     */
    public AbstractComponentConnector() {
    }

    /**
     * Creates and returns the widget for this VPaintableWidget. This method
     * should only be called once when initializing the paintable.
     * 
     * @return
     */
    protected abstract Widget createWidget();

    /**
     * Returns the widget associated with this paintable. The widget returned by
     * this method must not changed during the life time of the paintable.
     * 
     * @return The widget associated with this paintable
     */
    public Widget getWidget() {
        if (widget == null) {
            widget = createWidget();
        }

        return widget;
    }

    /**
     * Returns the shared state object for a paintable widget.
     * 
     * A new state instance is created using {@link #createState()} if none has
     * been set by the server.
     * 
     * If overriding this method to return a more specific type, also
     * {@link #createState()} must be overridden.
     * 
     * @return current shared state (not null)
     */
    public ComponentState getState() {
        if (state == null) {
            state = createState();
        }

        return state;
    }

    /**
     * Creates a new instance of a shared state object for the widget. Normally,
     * the state instance is created by the server and sent to the client before
     * being used - this method is used if no shared state has been sent by the
     * server.
     * 
     * When overriding {@link #getState()}, also {@link #createState()} should
     * be overridden to match it.
     * 
     * @return newly created component shared state instance
     */
    protected ComponentState createState() {
        return GWT.create(ComponentState.class);
    }

    protected static boolean isRealUpdate(UIDL uidl) {
        return !isCachedUpdate(uidl) && !uidl.getBooleanAttribute("invisible")
                && !uidl.hasAttribute("deferred");
    }

    protected static boolean isCachedUpdate(UIDL uidl) {
        return uidl.getBooleanAttribute("cached");
    }

    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        if (isCachedUpdate(uidl)) {
            return;
        }

        ConnectorMap paintableMap = ConnectorMap.get(getConnection());
        // register the listened events by the server-side to the event-handler
        // of the component
        paintableMap.registerEventListenersFromUIDL(getId(), uidl);

        // Visibility
        setVisible(!uidl.getBooleanAttribute("invisible"), uidl);

        if (uidl.getId().startsWith("PID_S")) {
            DOM.setElementProperty(getWidget().getElement(), "id", uidl.getId()
                    .substring(5));
        }

        if (!isVisible()) {
            // component is invisible, delete old size to notify parent, if
            // later made visible
            paintableMap.setOffsetSize(this, null);
            return;
        }

        if (!isRealUpdate(uidl)) {
            return;
        }

        /*
         * Disabled state may affect (override) tabindex so the order must be
         * first setting tabindex, then enabled state.
         */
        if (uidl.hasAttribute("tabindex") && getWidget() instanceof Focusable) {
            ((Focusable) getWidget()).setTabIndex(uidl
                    .getIntAttribute("tabindex"));
        }
        setEnabled(getState().isEnabled());

        // Style names
        String styleName = getStyleNameFromUIDL(getWidget()
                .getStylePrimaryName(), uidl, getState(),
                getWidget() instanceof Field);
        getWidget().setStyleName(styleName);

        // Update tooltip
        TooltipInfo tooltipInfo = paintableMap.getTooltipInfo(this, null);
        if (getState().hasDescription()) {
            tooltipInfo.setTitle(getState().getDescription());
        } else {
            tooltipInfo.setTitle(null);
        }
        // add error info to tooltip if present
        if (uidl.hasAttribute(ATTRIBUTE_ERROR)) {
            tooltipInfo.setErrorUidl(uidl.getErrors());
        } else {
            tooltipInfo.setErrorUidl(null);
        }

        // Set captions
        if (delegateCaptionHandling()) {
            getParent().updateCaption(this, uidl);
        }

        /*
         * updateComponentSize need to be after caption update so caption can be
         * taken into account
         */

        updateComponentSize();
    }

    private void updateComponentSize() {
        SharedState state = getState();

        String w = "";
        String h = "";
        if (null != state || !(state instanceof ComponentState)) {
            ComponentState componentState = (ComponentState) state;
            // TODO move logging to VUIDLBrowser and VDebugConsole
            // VConsole.log("Paintable state for "
            // + getPaintableMap().getPid(paintable) + ": "
            // + String.valueOf(state.getState()));
            h = componentState.getHeight();
            w = componentState.getWidth();
        } else {
            // TODO move logging to VUIDLBrowser and VDebugConsole
            VConsole.log("No state for paintable " + getId()
                    + " in VAbstractPaintableWidget.updateComponentSize()");
        }

        // Parent should be updated if either dimension changed between relative
        // and non-relative
        if (w.endsWith("%") != declaredWidth.endsWith("%")) {
            ComponentContainerConnector parent = getParent();
            if (parent instanceof ManagedLayout) {
                getLayoutManager().setWidthNeedsUpdate((ManagedLayout) parent);
            }
        }

        if (h.endsWith("%") != declaredHeight.endsWith("%")) {
            ComponentContainerConnector parent = getParent();
            if (parent instanceof ManagedLayout) {
                getLayoutManager().setHeightNeedsUpdate((ManagedLayout) parent);
            }
        }

        declaredWidth = w;
        declaredHeight = h;

        // Set defined sizes
        Widget component = getWidget();

        component.setStyleName("v-has-width", !isUndefinedWidth());
        component.setStyleName("v-has-height", !isUndefinedHeight());

        component.setHeight(h);
        component.setWidth(w);
    }

    public boolean isRelativeHeight() {
        return declaredHeight.endsWith("%");
    }

    public boolean isRelativeWidth() {
        return declaredWidth.endsWith("%");
    }

    public boolean isUndefinedHeight() {
        return declaredHeight.length() == 0;
    }

    public boolean isUndefinedWidth() {
        return declaredWidth.length() == 0;
    }

    public String getDeclaredHeight() {
        return declaredHeight;
    }

    public String getDeclaredWidth() {
        return declaredWidth;
    }

    /**
     * Sets the enabled state of this paintable
     * 
     * @param enabled
     *            true if the paintable is enabled, false otherwise
     */
    protected void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (getWidget() instanceof FocusWidget) {
            FocusWidget fw = (FocusWidget) getWidget();
            fw.setEnabled(enabled);
        }

    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Return true if parent handles caption, false if the paintable handles the
     * caption itself.
     * 
     * 
     * @deprecated This should always return true and all components should let
     *             the parent handle the caption and use other attributes for
     *             internal texts in the component
     * @return
     */
    @Deprecated
    protected boolean delegateCaptionHandling() {
        return true;
    }

    /**
     * Sets the visible state for this paintable.
     * 
     * @param visible
     *            true if the paintable should be made visible, false otherwise
     * @param captionUidl
     *            The UIDL that is passed to the parent and onwards to VCaption
     *            if the caption needs to be updated as a result of the
     *            visibility change.
     */
    protected void setVisible(boolean visible, UIDL captionUidl) {
        boolean wasVisible = this.visible;
        this.visible = visible;

        getWidget().setVisible(visible);
        if (wasVisible != visible) {
            // Changed invisibile <-> visible
            if (wasVisible && delegateCaptionHandling()) {
                // Must hide caption when component is hidden
                getParent().updateCaption(this, captionUidl);
            }
        }
    }

    protected boolean isVisible() {
        return visible;
    }

    /**
     * Generates the style name for the widget based on the given primary style
     * name (typically returned by Widget.getPrimaryStyleName()) and the UIDL
     * and shared state of the component. An additional "modified" style name
     * can be added if the field parameter is set to true.
     * 
     * @param primaryStyleName
     * @param uidl
     * @param state
     *            component shared state
     * @param field
     * @return
     */
    protected static String getStyleNameFromUIDL(String primaryStyleName,
            UIDL uidl, ComponentState state, boolean field) {
        boolean enabled = state.isEnabled();

        StringBuffer styleBuf = new StringBuffer();
        styleBuf.append(primaryStyleName);
        styleBuf.append(" v-paintable");

        // first disabling and read-only status
        if (!enabled) {
            styleBuf.append(" ");
            styleBuf.append(ApplicationConnection.DISABLED_CLASSNAME);
        }
        if (state.isReadOnly()) {
            styleBuf.append(" ");
            styleBuf.append("v-readonly");
        }

        // add additional styles as css classes, prefixed with component default
        // stylename
        if (state.hasStyles()) {
            final String[] styles = state.getStyle().split(" ");
            for (int i = 0; i < styles.length; i++) {
                styleBuf.append(" ");
                styleBuf.append(primaryStyleName);
                styleBuf.append("-");
                styleBuf.append(styles[i]);
                styleBuf.append(" ");
                styleBuf.append(styles[i]);
            }
        }

        // add modified classname to Fields
        if (field && uidl.hasAttribute("modified")) {
            styleBuf.append(" ");
            styleBuf.append(ApplicationConnection.MODIFIED_CLASSNAME);
        }

        // add error classname to components w/ error
        if (uidl.hasAttribute(ATTRIBUTE_ERROR)) {
            styleBuf.append(" ");
            styleBuf.append(primaryStyleName);
            styleBuf.append(ApplicationConnection.ERROR_CLASSNAME_EXT);
        }
        // add required style to required components
        if (uidl.hasAttribute(ATTRIBUTE_REQUIRED)) {
            styleBuf.append(" ");
            styleBuf.append(primaryStyleName);
            styleBuf.append(ApplicationConnection.REQUIRED_CLASSNAME_EXT);
        }

        return styleBuf.toString();
    }

    /**
     * Sets the shared state for the paintable widget.
     * 
     * @param new shared state (must be compatible with the return value of
     *        {@link #getState()} - {@link ComponentState} if
     *        {@link #getState()} is not overridden
     */
    public final void setState(SharedState state) {
        this.state = (ComponentState) state;
    }

    /**
     * Initialize the given RPC proxy object so it is connected to this
     * paintable.
     * 
     * @param clientToServerRpc
     *            The RPC instance to initialize. Must have been created using
     *            GWT.create().
     */
    protected <T extends ServerRpc> T initRPC(T clientToServerRpc) {
        ((InitializableClientToServerRpc) clientToServerRpc).initRpc(getId(),
                getConnection());
        return clientToServerRpc;
    }

    public LayoutManager getLayoutManager() {
        return LayoutManager.get(getConnection());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.gwt.client.Connector#getParent()
     */
    public ComponentContainerConnector getParent() {
        return parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.terminal.gwt.client.Connector#setParent(com.vaadin.terminal
     * .gwt.client.ComponentContainerConnector)
     */
    public void setParent(ComponentContainerConnector parent) {
        this.parent = parent;
    }

}