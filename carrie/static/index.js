var show_timer;
var hide_timer;

set_status = function(message) {
	clearTimeout(show_timer);
	clearTimeout(hide_timer);
	show_timer = setTimeout(function() {
		$("#status").css("display", "none").html(message).fadeIn(100);
		hide_timer = setTimeout(function () {
			$("#status").fadeOut(1000);
		}, 2000);
	}, 200);
};

send = function(command) {
	return function () {
		set_status("sending...");
		$.ajax({
			url: command,
			success: function (rawresponse) {
				set_status(rawresponse);
			},
			fail: function (xhr, error) {
				set_status("error: " + error);
			},
		});
	};
};

$(function () {
	$("div").buttonset();
	$("span").buttonset();
	$(".fakebutton").button().attr("disabled", "true");
	$("#pause").button({icons: {primary:'ui-icon-pause', secondary:'ui-icon-play'}}).on("click", send("pause"));
	$("#backward").button({icons: {primary:'ui-icon-seek-prev'}}).on("click", send("backward/12"));
	$("#forward").button({icons: {secondary:'ui-icon-seek-next'}}).on("click", send("forward/12"));
	$("#bbackward").button({icons: {primary:'ui-icon-first'}}).on("click", send("backward/180"));
	$("#fforward").button({icons: {secondary:'ui-icon-last'}}).on("click", send("forward/180"));
	$("#voldown").button({icons: {primary:'ui-icon-minus'}}).on("click", send("voldown"));
	$("#mute").button({icons: {secondary:'ui-icon-signal-diag'}}).on("click", send("mute"));
	$("#volup").button({icons: {secondary:'ui-icon-plus'}}).on("click", send("volup"));
	$("#osdon").button({icons: {primary:'ui-icon-bullet'}}).on("click", send("osdon"));
	$("#osdoff").button({icons: {secondary:'ui-icon-radio-on'}}).on("click", send("osdoff"));
	$("#fullscreen").button({icons: {secondary:'ui-icon-lightbulb'}}).on("click", send("fullscreen"));
});
