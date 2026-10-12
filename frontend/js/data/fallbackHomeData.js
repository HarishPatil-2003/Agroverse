export const fallbackHomeSummary = {
  activeCrops: 12,
  activeListings: 8,
  newNotifications: 3,
  tasksToday: 5
};

export const fallbackCrops = [
  {
    id: 1,
    name: "Tomato",
    field: "Field A-12",
    status: "Healthy",
    image: "assets/tomato.png",
    lastChecked: "2h ago"
  },
  {
    id: 2,
    name: "Wheat",
    field: "North Field",
    status: "Good",
    image: "assets/wheat.png",
    lastChecked: "5h ago"
  },
  {
    id: 3,
    name: "Corn",
    field: "East Zone",
    status: "Needs Attention",
    image: "assets/corn.png",
    lastChecked: "1d ago"
  }
];

export const fallbackTasks = [
  {
    id: 1,
    title: "Irrigation Due",
    subtitle: "Field A-12 • Tomato",
    time: "08:30 AM",
    type: "water",
    icon: "fa-tint",
    bg: "#e3f2fd",
    color: "#1565c0"
  },
  {
    id: 2,
    title: "Fertilizer Schedule",
    subtitle: "North Field • Wheat",
    time: "11:00 AM",
    type: "fertilizer",
    icon: "fa-flask",
    bg: "#fff3e0",
    color: "#ef6c00"
  },
  {
    id: 3,
    title: "Scheme Deadline",
    subtitle: "Govt Subsidy Form",
    time: "05:00 PM",
    type: "scheme",
    icon: "fa-clock",
    bg: "#f3e5f5",
    color: "#7b1fa2"
  }
];

export const fallbackCommunitySnapshot = {
  author: "Rajesh Kumar",
  time: "2 hours ago",
  text: "\"Does anyone have experience with organic pesticides for tomato blight? My crops in Nashik area are showing early signs of yellowing.\"",
  likes: 12,
  comments: 5,
  avatar: "https://ui-avatars.com/api/?name=Rajesh+Kumar&background=3498db&color=fff"
};

export const fallbackMarketplaceSummary = {
  totalProducts: 15,
  activeListings: 8,
  recentProduct: "Organic Fertilizer"
};
