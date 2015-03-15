var discussionThreadId = "";
var isReplyTagCreated = new Boolean(false);
var currentCommentIdForReplying = "";
var addReplyToButtonId = "";
var ajaxDataToSend = "";

// #66FF33 - Olive Green
// #DE6B1F - Decent red
// #6600FF - Violet Blue
// #FF6600 - brick Orange
// #B8A565 - Safari Green

var coloursArray = ["#66FF33", "#DE6B1F", "#6600FF", "#FF6600", "#B8A565"];
function getColourCode(commentId) {
 var noOfColoursAvailable = coloursArray.length;
 var noOfHyphens = commentId.split("-").length - 1;
 var moduloCount = noOfHyphens % noOfColoursAvailable;
 return coloursArray[moduloCount - 1];
}
$(function() {
   discussionThreadId = $('#discussionThreadId').html();
   ajaxCall();
});

var ajaxCall = function() {
    var ajaxCallBack = {
            data : "Some Data",
            type : "POST",
            success : onSuccessInit,
            error : onError
        }
    jsRoutes.controllers.DiscussionBoardApplication.ajaxCall(discussionThreadId).ajax(ajaxCallBack);
};

var replyAjaxCall = function() {
    var replyAjaxCallBack = {
              data : "Some Data",
              type : "POST",
              success : onSuccessAddReply,
              error : onError
       }
    jsRoutes.controllers.DiscussionBoardApplication.addReplyCommentAjaxCall(ajaxDataToSend).ajax(replyAjaxCallBack);
};

var  onSuccessInit = function(data) {
    $('#threadContent').append("Title :  " + $('#discussionThreadTitle').html() + "<br />")
    $('#threadContent').append("Thread Started By:  " + data.postedBy + "<br />")
    $('#threadContent').append("Date/Time Posted: " + convertUTCToDateTime(data.createdTime) + "<br />")
    $('#threadContent').append("No. of replies: " + data.replies.length + "<br />")

    var discussionHTML = "<table style='width:400px' border=1 " + "id = " + "table-" + $('#discussionThreadId').html() +
                             "><tr><td>" +  data.commentData + "</td></tr>" +
                             "<tr><td align='left'><input type='submit' onclick=ajaxAddReply(this) value='Reply' id=replyButton-" + $('#discussionThreadId').html() + " /></td></tr>" +
                             "</table>";
    $('#threadContent').append(discussionHTML)

    appendRepliesToDOM(data.replies);

}

var onError = function(error) {
    alert(error);
}

 var  onSuccessAddReply = function(data) {
    cancelAjaxReply();
    appendRepliesToDOM(data)
  }



function convertUTCToDateTime(UTCTimestamp) {
    var convertdLocalTime = new Date(UTCTimestamp).toISOString();
    convertdLocalTime = convertdLocalTime.replace("T", " ")
    convertdLocalTime = convertdLocalTime.replace("Z", "")
    return convertdLocalTime;
}

function appendRepliesToDOM(repliesList) {
    $.each(repliesList, function(index, element) {
        var parentId = "#table-" + getParentId(element.commentId) ;
        //alert(parentId);
        var newTextHtml = "<ul><table><tr height='5px'><td></td></tr></table>" +
                                "<table style='width:400px' bgcolor=" + getColourCode(element.commentId) + " id = " + "table-" + element.commentId +
                                    "><tr><td>" + element.commentData + "</td></tr>" +
                                    "<tr><td align='right'>Posted By: " + element.postedBy + "</td></tr>" +
                                    "<tr><td align='right'>Posted At: " + convertUTCToDateTime(element.createdTime) + "</td></tr>" +
                                    "<tr><td align='left'><input type='submit' onclick=ajaxAddReply(this) value='Reply' id=replyButton-" + element.commentId  + " /></td></tr>" +
                                    "</table></ul>"
        $( newTextHtml ).insertAfter( parentId );

    });

}

function getParentId(commentId) {
    var lastIndex = commentId.lastIndexOf("-");
    var result = commentId.substring(0, lastIndex);
    return result;
}

function ajaxAddReply(thisObj) {
 if(isReplyTagCreated == false) {
       var replyButtonId = $(thisObj).attr('id');
       var firstIndex = replyButtonId.indexOf("-");
       addReplyToButtonId = replyButtonId.substring(firstIndex + 1, replyButtonId.length)
       //alert('replyButtonId = ' + replyButtonId + ":::" + 'buttonId = ' + buttonId)
       var newTextHtml = "<ul id='tempULTag'><table style='width:400px' frame='box'>" +
                                           "<tr><td> Name : &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type='text' id='tempReplyName' /></td></tr>" +
                                           "<tr><td valign='top'> Reply Text: <textarea rows=4 cols=40 id='tempReplyText' /></td></tr>" +
                                           "<tr><td align='left'><input type='submit' onclick=sendAjaxReply(this) value='Post'/>&nbsp;&nbsp;<input type='submit' onclick=cancelAjaxReply() value='Cancel'/></td></tr>" +
                                           "</table></ul>"
       //alert('#table-' + buttonId);
       $( newTextHtml ).insertAfter( '#table-' + addReplyToButtonId );
       isReplyTagCreated = true;
   }
}

function sendAjaxReply(thisObj) {
 var replyAuthorsName = $('#tempReplyName').val().trim();
 var replyTextToSubmit = $('#tempReplyText').val().trim();
 if(replyAuthorsName.length !=5 || !(replyTextToSubmit.length  >= 10 && replyTextToSubmit.length < 100)) {
    alert("Authors Name should have exactly 5 chars and reply should be between 10 and 100 chars")
 }
 else {
    ajaxDataToSend = addReplyToButtonId + "::::" + replyAuthorsName + "::::"  + replyTextToSubmit ;
    replyAjaxCall();
 }
}

function cancelAjaxReply() {
    isReplyTagCreated = false;
    $('#tempULTag').remove();

}
