package org.aisen.weibo.sina.ui.fragment.profile;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.aisen.android.common.utils.Utils;
import org.aisen.android.component.bitmaploader.BitmapLoader;
import org.aisen.android.component.bitmaploader.core.ImageConfig;
import org.aisen.android.component.bitmaploader.display.DefaultDisplayer;
import org.aisen.android.network.task.TaskException;
import org.aisen.android.network.task.WorkTask;
import org.aisen.android.support.bean.TabItem;
import org.aisen.android.support.inject.InjectUtility;
import org.aisen.android.support.inject.ViewInject;
import org.aisen.android.ui.activity.basic.BaseActivity;
import org.aisen.android.ui.activity.container.FragmentArgs;
import org.aisen.android.ui.fragment.ATabsTabLayoutFragment;
import org.aisen.weibo.sina.R;
import org.aisen.weibo.sina.base.AppContext;
import org.aisen.weibo.sina.sinasdk.SinaSDK;
import org.aisen.weibo.sina.sinasdk.bean.FriendshipShow;
import org.aisen.weibo.sina.sinasdk.bean.Token;
import org.aisen.weibo.sina.sinasdk.bean.WeiBoUser;
import org.aisen.weibo.sina.support.bean.AccountBean;
import org.aisen.weibo.sina.support.utils.AccountUtils;
import org.aisen.weibo.sina.support.utils.AisenUtils;
import org.aisen.weibo.sina.support.utils.ImageConfigUtils;
import org.aisen.weibo.sina.ui.activity.base.SinaCommonActivity;
import org.aisen.weibo.sina.ui.fragment.base.BizFragment;
import org.aisen.weibo.sina.ui.fragment.timeline.TimelineDefFragment;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by wangdan on 16/1/12.
 */
public class ProfilePagerFragment extends ATabsTabLayoutFragment<TabItem>
                                    implements View.OnClickListener,
                                        BizFragment.OnCreateFriendshipCallback,
                                        BizFragment.OnDestoryFriendshipCallback,
                                        BizFragment.OnDestoryFollowerCallback {

    public static void launch(Activity from, WeiBoUser user) {
        FragmentArgs args = new FragmentArgs();
        args.add("mUser", user);
        args.add(SET_INDEX, String.valueOf(1));

        SinaCommonActivity.launch(from, ProfilePagerFragment.class, args);
    }

    // 封面
    @ViewInject(id = R.id.imgCover)
    ImageView imgCover;
    // 名字
    @ViewInject(id = R.id.txtName)
    TextView txtName;
    // 头像
    @ViewInject(id = R.id.imgAvatar)
    ImageView imgAvatar;
    // 认证类别
    @ViewInject(id = R.id.imgVerified)
    ImageView imgVerified;
    // 性别
    @ViewInject(id = R.id.imgGender)
    ImageView imgGender;
    // 粉丝数
    @ViewInject(id = R.id.txtFollowersCounter, click = "onClick")
    TextView txtFollowersCounter;
    // 关注数
    @ViewInject(id = R.id.txtFriendsCounter, click = "onClick")
    TextView txtFriendsCounter;
    // 简介
    @ViewInject(id = R.id.txtDesc)
    TextView txtDesc;

    private WeiBoUser mUser;
    private FriendshipShow mFriendship;

    @Override
    public int inflateContentView() {
        return -1;
    }

    @Override
    public int inflateActivityContentView() {
        return R.layout.ui_profile_pager;
    }

    public int setActivityTheme() {
        return R.style.Profile;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        InjectUtility.initInjectedView(this, ((BaseActivity) getActivity()).getRootView());
        layoutInit(inflater, savedInstanceState);

        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUser = savedInstanceState == null ? (WeiBoUser) getArguments().getSerializable("mUser")
                                           : (WeiBoUser) savedInstanceState.getSerializable("mUser");

        BizFragment.getBizFragment(getActivity());
    }

    @Override
    protected void setupTabLayout(Bundle savedInstanceSate, TabLayout tabLayout) {
        super.setupTabLayout(savedInstanceSate, tabLayout);

        tabLayout.setPadding(Utils.dip2px(16), tabLayout.getPaddingTop(), tabLayout.getPaddingRight(), tabLayout.getPaddingBottom());
    }

    @Override
    protected void layoutInit(LayoutInflater inflater, Bundle savedInstanceSate) {
        super.layoutInit(inflater, savedInstanceSate);

        if (savedInstanceSate == null)
            new RefreshProfileTask().execute();

        setProfile();

        BaseActivity activity = (BaseActivity) getActivity();
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowHomeEnabled(false);
        activity.getSupportActionBar().setTitle("");
        activity.getToolbar().setBackgroundColor(Color.TRANSPARENT);

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("mUser", mUser);
    }

    @Override
    protected ArrayList<TabItem> generateTabs() {
        ArrayList<TabItem> tabItems = new ArrayList<>();

        tabItems.add(new TabItem("1", "关于"));
        tabItems.add(new TabItem("2", "微博"));

        return tabItems;
    }

    @Override
    protected Fragment newFragment(TabItem bean) {
        int type = Integer.parseInt(bean.getType());

        if (type == 1) {
            return ProfileAboutFragment.newInstance(mUser);
        }
        else if (type == 2) {
            return TimelineDefFragment.newInstance("statusesFriendsTimeLine");
        }

        return null;
    }

    private void setProfile() {
        // 封面
        ImageConfig coverConfig = new ImageConfig();
        coverConfig.setLoadfaildRes(R.drawable.bg_banner_dialog);
        coverConfig.setLoadingRes(R.drawable.bg_banner_dialog);
        coverConfig.setDisplayer(new DefaultDisplayer());
        BitmapLoader.getInstance().display(this, mUser.getCover_image_phone(), imgCover, coverConfig);
        // 名字
        // fuck 2014-09-04 当名字过长大于8个字时，截取部分文字
        int maxLength = AisenUtils.getStrLength("一二三四五六七八九十");
        if (AisenUtils.getStrLength(mUser.getName()) > maxLength) {
            StringBuffer sb = new StringBuffer();
            int index = 0;
            while (AisenUtils.getStrLength(sb.toString()) < maxLength) {
                if (index >= mUser.getName().length())
                    break;

                sb.append(mUser.getName().charAt(index));
                index++;
            }
            sb.append("...");
            txtName.setText(sb.toString());
        }
        else {
            txtName.setText(mUser.getScreen_name());
        }
        // 头像
        BitmapLoader.getInstance().display(this, AisenUtils.getUserPhoto(mUser), imgAvatar, ImageConfigUtils.getLargePhotoConfig());
        // 性别
        imgGender.setVisibility(View.VISIBLE);
        if ("m".equals(mUser.getGender()))
            imgGender.setImageResource(R.drawable.list_male);
        else if ("f".equals(mUser.getGender()))
            imgGender.setImageResource(R.drawable.list_female);
        else
            imgGender.setVisibility(View.GONE);
        // 认证
        AisenUtils.setImageVerified(imgVerified, mUser);
        // 关注数
        txtFriendsCounter.setText(String.format(getString(R.string.profile_friends), AisenUtils.getCounter(mUser.getFriends_count())));
        // 粉丝数
        txtFollowersCounter.setText(String.format(getString(R.string.profile_followers), AisenUtils.getCounter(mUser.getFollowers_count())));
        // 简介
        txtDesc.setText(mUser.getDescription());
        // 简介
        if (!TextUtils.isEmpty(mUser.getDescription()))
            txtDesc.setText(mUser.getDescription());
        else
            txtDesc.setText(getString(R.string.profile_des_none));
    }

    @Override
    public void onClick(View v) {
        // 关注
        if (v.getId() == R.id.txtFriendsCounter) {
//            FriendshipTabsFragment.launch(getActivity(), mUser, 0);
        }
        // 粉丝
        else if (v.getId() == R.id.txtFollowersCounter) {
//            FriendshipTabsFragment.launch(getActivity(), mUser, 1);
        }
    }

//    @Override
//    public void onRefresh() {
//        Fragment fragment = getCurrentFragment();
//        if (fragment instanceof IUserProfileRefresh) {
//            // 刷新用户关系
//            ((IUserProfileRefresh) fragment).refreshProfile();
//
//            // 刷新用户详情资料
//            new RefreshProfileTask().execute();
//        }
//    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_profile_pager, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem createItem = menu.findItem(R.id.create);
        MenuItem destoryItem = menu.findItem(R.id.destory);
        MenuItem followerDestoryItem = menu.findItem(R.id.followDestory);

        if (mUser == null || AppContext.getAccount().getUser().getIdstr().equals(mUser.getIdstr())) {
            createItem.setVisible(false);
            destoryItem.setVisible(false);
            followerDestoryItem.setVisible(false);
        }
        else {
            createItem.setVisible(mFriendship != null && !mFriendship.getSource().getFollowing());
            destoryItem.setVisible(mFriendship != null && mFriendship.getSource().getFollowing());
            followerDestoryItem.setVisible(mFriendship != null && mFriendship.getTarget().getFollowing());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 关注
        if (item.getItemId() == R.id.create) {
            BizFragment.getBizFragment(this).createFriendship(mUser, this);
        }
        // 取消关注
        else if (item.getItemId() == R.id.destory) {
            BizFragment.getBizFragment(this).destoryFriendship(mUser, this);
        }
        // 移除粉丝
        else if (item.getItemId() == R.id.followDestory) {
            BizFragment.getBizFragment(this).destoryFollower(mUser, this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFriendshipCreated(WeiBoUser targetUser) {
        getProfileFragment().onFriendshipCreated(targetUser);
    }

    @Override
    public void onFriendshipDestoryed(WeiBoUser targetUser) {
        getProfileFragment().onFriendshipDestoryed(targetUser);
    }

    @Override
    public void onDestoryFollower(WeiBoUser user) {
        getProfileFragment().onDestoryFollower(user);
    }

    private ProfileAboutFragment getProfileFragment() {
        if (getFragments() == null)
            return null;

        Set<String> keyset = getFragments().keySet();
        for (String key : keyset) {
            Fragment fragment = getFragments().get(key);
            if (fragment instanceof ProfileAboutFragment) {
                ProfileAboutFragment tab1Fragment = (ProfileAboutFragment) fragment;
                return tab1Fragment;
            }
        }

        return null;
    }

    // 刷新用户信息
    class RefreshProfileTask extends WorkTask<Void, Void, WeiBoUser> {

        @Override
        public WeiBoUser workInBackground(Void... params) throws TaskException {
            Token token = AppContext.getAccount().getAdvancedToken();

            WeiBoUser userInfo = SinaSDK.getInstance(token).userShow(mUser.getIdstr(), null);

            if (AppContext.isLoggedIn() && userInfo.getIdstr().equals(AppContext.getAccount().getUser().getIdstr())) {
                // 更新DB
                AccountBean bean = new AccountBean();
                bean.setUid(userInfo.getIdstr());
                bean.setGroups(AppContext.getAccount().getGroups());
                bean.setUser(userInfo);
                AccountUtils.newAccount(bean);

                // 更新内存
                AppContext.getAccount().setUser(userInfo);
            }

            return userInfo;
        }

        @Override
        protected void onSuccess(WeiBoUser result) {
            super.onSuccess(result);

            if (getActivity() == null || result == null)
                return;

            // 有些时候获取不到用户信息了
            // 艹他妈的新浪，remark都不让获取了
            if (!TextUtils.isEmpty(result.getRemark()))
                mUser.setRemark(result.getRemark());
            if (result.getFollowers_count() > 0)
                mUser.setFollowers_count(result.getFollowers_count());
            if (result.getFriends_count() > 0)
                mUser.setFriends_count(result.getFriends_count());
            if (!TextUtils.isEmpty(result.getCover_image_phone()))
                mUser.setCover_image_phone(result.getCover_image_phone());
            if (!TextUtils.isEmpty(result.getDescription()))
                mUser.setDescription(result.getDescription());
            if (!TextUtils.isEmpty(result.getVerified_reason()))
                mUser.setVerified_reason(result.getVerified_reason());

            setProfile();
            ProfileAboutFragment fragment = getProfileFragment();
            if (fragment != null)
                fragment.setUser(mUser);
        }

        @Override
        protected void onFailure(TaskException exception) {
            super.onFailure(exception);

            showMessage(exception.getMessage());
        }

    }

    public void setFriendshipShow(FriendshipShow friendship) {
        this.mFriendship = friendship;

        getActivity().invalidateOptionsMenu();
    }

    public interface IUserProfileRefresh {

        public void refreshProfile();

    }

}
