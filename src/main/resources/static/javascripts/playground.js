$(function () {

    // show the correct step
    $(function () {
        $('#' + $('input#step').val()).collapse('show');
    });

    var isImplicitGrant = document.getElementById("implicitGrant").checked;
    if (isImplicitGrant) {
        $('[data-show-implicit="false"]').hide();
        $('[data-show-auth-code="false"]').show();
    } else {
        $('[data-show-implicit="false"]').show();
        $('[data-show-auth-code="false"]').hide();
    }

    // changing the grant type means toggling some fields:
    $("input[name='grantType']").click(function () {
        $('[data-show-client-credentials]').toggle(this.id !== 'clientCredentials');
        $('[data-show-implicit]').toggle(this.id !== 'implicitGrant');
        $('[data-show-auth-code]').toggle(this.id !== 'authCode');

        var responseTypeSelector = this.id === 'implicitGrant' ? "idTokenToken" : "code";
        $('#' + responseTypeSelector).prop("checked", true);

        var responseModeSelector = this.id === 'implicitGrant' ? "fragment" : "query";
        $('#' + responseModeSelector).prop("checked", true);
    });


    // we are in step 3 of implicit grant
    if ($('#parseAnchorForAccessToken').val() == 'true') {
        var hash = window.location.hash.slice(1);
        $('#parseAnchorForAccessToken').val('');

        $('#responseInfo').html(hash);
        var accessToken;
        $.each(hash.split("&"), function (i, value) {
            var param = value.split("=");
            if (param[0] == 'access_token') {
                accessToken = param[1];
                $('#accessToken').val(accessToken);
            }
        });
        if ($('#openIdConnect').val() == 'true') {
            var jqxhrAt = $.get("decodeJwtToken?jwtToken=" + accessToken).done(function () {
                $('#accessTokenJsonPrint').html(jqxhrAt.responseText);
                $('#accessTokenJsonPrintInput').val(jqxhrAt.responseText)
            });
        }
    }

    // we are in step 3 of id_token
    if ($('#parseAnchorForIdToken').val() == 'true') {
        var hash = window.location.hash.slice(1);
        $('#parseAnchorForIdToken').val('');

        $('#responseInfo').html(hash.split("&").join("<p/>"));
        var oidcIdToken;
        $.each(hash.split("&"), function (i, value) {
            var param = value.split("=");
            if (param[0] == 'id_token') {
                oidcIdToken = param[1];
                $('#oidcIdToken').val(oidcIdToken);
            }
        });
        var jqxhrOidc = $.get("decodeJwtToken?jwtToken=" + oidcIdToken).done(function () {
            $('#idTokenJsonPrint').html(jqxhrOidc.responseText);
            $('#idTokenJsonPrintInput').val(jqxhrOidc.responseText)
        });
    }

});
